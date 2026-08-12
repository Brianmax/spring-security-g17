package com.jwt.codigo;

import com.jwt.codigo.client.ExchangeRateClient;
import com.jwt.codigo.dto.account.AccountResponse;
import com.jwt.codigo.dto.account.CreateAccountRequest;
import com.jwt.codigo.dto.common.PageResponse;
import com.jwt.codigo.dto.transaction.MoneyOperationRequest;
import com.jwt.codigo.dto.transaction.TransactionResponse;
import com.jwt.codigo.dto.transfer.CreateTransferRequest;
import com.jwt.codigo.dto.user.CreateUserRequest;
import com.jwt.codigo.dto.user.UpdateUserRequest;
import com.jwt.codigo.dto.user.UserResponse;
import com.jwt.codigo.entity.BankAccountEntity;
import com.jwt.codigo.enums.AccountStatus;
import com.jwt.codigo.enums.AccountType;
import com.jwt.codigo.enums.CurrencyCode;
import com.jwt.codigo.enums.TransactionType;
import com.jwt.codigo.enums.UserStatus;
import com.jwt.codigo.exception.ApiException;
import com.jwt.codigo.repository.AccountTransactionRepository;
import com.jwt.codigo.repository.BankAccountRepository;
import com.jwt.codigo.repository.TransferRepository;
import com.jwt.codigo.repository.UserRepository;
import com.jwt.codigo.service.AccountOperationService;
import com.jwt.codigo.service.AccountService;
import com.jwt.codigo.service.TransactionService;
import com.jwt.codigo.service.TransferService;
import com.jwt.codigo.service.UserService;
import com.jwt.codigo.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class BankingIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountOperationService operationService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountRepository accountRepository;

    @Autowired
    private AccountTransactionRepository transactionRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExchangeRateClient exchangeRateClient;

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        transferRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
        when(exchangeRateClient.getAverageRate(CurrencyCode.USD)).thenReturn(
                new ExchangeRateClient.ExchangeRateQuote(
                        CurrencyCode.USD,
                        new BigDecimal("3.5000"),
                        new BigDecimal("3.6000"),
                        LocalDate.of(2026, 8, 3)
                )
        );
        when(exchangeRateClient.getAverageRate(CurrencyCode.EUR)).thenReturn(
                new ExchangeRateClient.ExchangeRateQuote(
                        CurrencyCode.EUR,
                        new BigDecimal("4.0000"),
                        new BigDecimal("4.2000"),
                        LocalDate.of(2026, 8, 3)
                )
        );
    }

    @Test
    void createsAndUpdatesUserAndCreatesAccount() {
        UserResponse user = createUser("owner@example.com");
        UserResponse updated = userService.update(
                user.id(),
                new UpdateUserRequest("Grace", "Hopper", "grace@example.com", UserStatus.ACTIVE)
        );
        AccountResponse account = createAccount(updated.id(), CurrencyCode.USD);

        assertThat(updated.firstName()).isEqualTo("Grace");
        assertThat(updated.email()).isEqualTo("grace@example.com");
        assertThat(account.ownerId()).isEqualTo(updated.id());
        assertThat(account.balance()).isEqualByComparingTo("0.0000");
    }

    @Test
    void rejectsDuplicateUserEmail() {
        createUser("owner@example.com");

        assertApiCode(
                () -> createUser("OWNER@example.com"),
                "DUPLICATE_EMAIL"
        );
    }

    @Test
    void depositsAndWithdrawsAndCreatesImmutableLedgerEntries() {
        AccountResponse account = createUsdAccount("money@example.com");

        TransactionResponse deposit = operationService.deposit(
                account.id(), new MoneyOperationRequest(new BigDecimal("100.00"), "Initial deposit")
        );
        TransactionResponse withdrawal = operationService.withdraw(
                account.id(), new MoneyOperationRequest(new BigDecimal("40.00"), "ATM")
        );

        assertThat(deposit.transactionType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(deposit.balanceAfterTransaction()).isEqualByComparingTo("100.0000");
        assertThat(withdrawal.transactionType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(withdrawal.balanceAfterTransaction()).isEqualByComparingTo("60.0000");
        assertBalance(account.id(), "60.0000");
        assertThat(transactionRepository.countByAccountId(account.id())).isEqualTo(2);
    }

    @Test
    void rejectsInsufficientFundsAndRollsBackWithdrawal() {
        AccountResponse account = createUsdAccount("withdrawal@example.com");
        operationService.deposit(account.id(), money("25.00"));

        assertApiCode(() -> operationService.withdraw(account.id(), money("30.00")), "INSUFFICIENT_FUNDS");

        assertBalance(account.id(), "25.0000");
        assertThat(transactionRepository.countByAccountId(account.id())).isEqualTo(1);
    }

    @Test
    void rejectsDepositAndWithdrawalOnFrozenAccount() {
        AccountResponse account = createUsdAccount("frozen@example.com");
        operationService.deposit(account.id(), money("20.00"));
        accountService.freeze(account.id());

        assertApiCode(() -> operationService.deposit(account.id(), money("1.00")), "ACCOUNT_NOT_ACTIVE");
        assertApiCode(() -> operationService.withdraw(account.id(), money("1.00")), "ACCOUNT_NOT_ACTIVE");

        assertBalance(account.id(), "20.0000");
        assertThat(transactionRepository.countByAccountId(account.id())).isEqualTo(1);
    }

    @Test
    void transfersBetweenAccountsAndCreatesBothLedgerEntries() {
        UserResponse user = createUser("transfer@example.com");
        AccountResponse source = createAccount(user.id(), CurrencyCode.USD);
        AccountResponse destination = createAccount(user.id(), CurrencyCode.USD);
        operationService.deposit(source.id(), money("100.00"));

        TransferService.TransferResult result = transferService.transfer(transferRequest(
                source.id(), destination.id(), "40.00", "transfer-success"
        ));

        assertThat(result.created()).isTrue();
        assertThat(result.response().status().name()).isEqualTo("COMPLETED");
        assertThat(result.response().destinationAmount()).isEqualByComparingTo("40.0000");
        assertThat(result.response().exchangeRate()).isEqualByComparingTo("1.00000000");
        assertThat(result.response().exchangeRateProvider()).isEqualTo("INTERNAL");
        assertThat(transferService.findById(result.response().id()).status().name()).isEqualTo("COMPLETED");
        assertBalance(source.id(), "60.0000");
        assertBalance(destination.id(), "40.0000");
        assertThat(transactionRepository.countByAccountId(source.id())).isEqualTo(2);
        assertThat(transactionRepository.countByAccountId(destination.id())).isEqualTo(1);
    }

    @Test
    void convertsTransferBetweenDifferentCurrencies() {
        UserResponse user = createUser("currency@example.com");
        AccountResponse source = createAccount(user.id(), CurrencyCode.USD);
        AccountResponse destination = createAccount(user.id(), CurrencyCode.EUR);
        operationService.deposit(source.id(), money("100.00"));

        TransferService.TransferResult result = transferService.transfer(
                transferRequest(source.id(), destination.id(), "10.00", "currency")
        );

        assertThat(result.response().amount()).isEqualByComparingTo("10.0000");
        assertThat(result.response().destinationAmount()).isEqualByComparingTo("8.3333");
        assertThat(result.response().sourceCurrency()).isEqualTo(CurrencyCode.USD);
        assertThat(result.response().destinationCurrency()).isEqualTo(CurrencyCode.EUR);
        assertThat(result.response().exchangeRate()).isEqualByComparingTo("0.83333000");
        assertThat(result.response().exchangeRateDate()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(result.response().exchangeRateProvider()).isEqualTo("DECOLECTA_SBS_AVERAGE");
        assertBalance(source.id(), "90.0000");
        assertBalance(destination.id(), "8.3333");
        assertThat(transactionRepository.countByAccountId(destination.id())).isEqualTo(1);
    }

    @Test
    void exchangeRateProviderFailureRollsBackCrossCurrencyTransfer() {
        UserResponse user = createUser("provider-failure@example.com");
        AccountResponse source = createAccount(user.id(), CurrencyCode.USD);
        AccountResponse destination = createAccount(user.id(), CurrencyCode.EUR);
        operationService.deposit(source.id(), money("100.00"));
        when(exchangeRateClient.getAverageRate(CurrencyCode.USD)).thenThrow(new ApiException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                "EXCHANGE_RATE_PROVIDER_UNAVAILABLE",
                "Provider unavailable"
        ));

        assertApiCode(
                () -> transferService.transfer(transferRequest(source.id(), destination.id(), "10.00", "provider-failure")),
                "EXCHANGE_RATE_PROVIDER_UNAVAILABLE"
        );

        assertBalance(source.id(), "100.0000");
        assertBalance(destination.id(), "0.0000");
        assertThat(transferRepository.count()).isZero();
        assertThat(transactionRepository.countByAccountId(source.id())).isEqualTo(1);
    }

    @Test
    void failedTransferRollsBackAllChanges() {
        UserResponse user = createUser("rollback@example.com");
        AccountResponse source = createAccount(user.id(), CurrencyCode.USD);
        AccountResponse destination = createAccount(user.id(), CurrencyCode.USD);
        operationService.deposit(source.id(), money("20.00"));

        assertApiCode(
                () -> transferService.transfer(transferRequest(source.id(), destination.id(), "21.00", "rollback")),
                "INSUFFICIENT_FUNDS"
        );

        assertBalance(source.id(), "20.0000");
        assertBalance(destination.id(), "0.0000");
        assertThat(transferRepository.count()).isZero();
        assertThat(transactionRepository.countByAccountId(source.id())).isEqualTo(1);
        assertThat(transactionRepository.countByAccountId(destination.id())).isZero();
    }

    @Test
    void rejectsTransferInvolvingFrozenAccountAndSameAccount() {
        UserResponse user = createUser("transfer-rules@example.com");
        AccountResponse source = createAccount(user.id(), CurrencyCode.USD);
        AccountResponse destination = createAccount(user.id(), CurrencyCode.USD);
        operationService.deposit(source.id(), money("50.00"));
        accountService.freeze(destination.id());

        assertApiCode(
                () -> transferService.transfer(transferRequest(source.id(), destination.id(), "10.00", "frozen-transfer")),
                "ACCOUNT_NOT_ACTIVE"
        );
        assertApiCode(
                () -> transferService.transfer(transferRequest(source.id(), source.id(), "10.00", "same-transfer")),
                "SAME_ACCOUNT_TRANSFER"
        );

        assertBalance(source.id(), "50.0000");
    }

    @Test
    void duplicateIdempotencyKeyReturnsOriginalTransferWithoutMovingMoneyAgain() {
        UserResponse user = createUser("idempotency@example.com");
        AccountResponse source = createAccount(user.id(), CurrencyCode.USD);
        AccountResponse destination = createAccount(user.id(), CurrencyCode.USD);
        operationService.deposit(source.id(), money("50.00"));
        CreateTransferRequest request = transferRequest(source.id(), destination.id(), "10.00", "one-transfer");

        TransferService.TransferResult first = transferService.transfer(request);
        TransferService.TransferResult replay = transferService.transfer(request);

        assertThat(first.created()).isTrue();
        assertThat(replay.created()).isFalse();
        assertThat(replay.response().id()).isEqualTo(first.response().id());
        assertBalance(source.id(), "40.0000");
        assertBalance(destination.id(), "10.0000");
        assertThat(transferRepository.count()).isEqualTo(1);
    }

    @Test
    void concurrentWithdrawalsCannotOverdrawAccount() throws Exception {
        AccountResponse account = createUsdAccount("concurrent-withdrawal@example.com");
        operationService.deposit(account.id(), money("100.00"));

        List<Object> results = executeConcurrently(
                () -> operationService.withdraw(account.id(), money("80.00")),
                () -> operationService.withdraw(account.id(), money("80.00"))
        );

        assertThat(results.stream().filter(TransactionResponse.class::isInstance)).hasSize(1);
        assertThat(results.stream().filter(ApiException.class::isInstance)
                .map(ApiException.class::cast).map(ApiException::getCode)).containsExactly("INSUFFICIENT_FUNDS");
        assertBalance(account.id(), "20.0000");
    }

    @Test
    void concurrentTransfersCannotOverspendAndDoNotDeadlock() throws Exception {
        UserResponse user = createUser("concurrent-transfer@example.com");
        AccountResponse source = createAccount(user.id(), CurrencyCode.USD);
        AccountResponse destinationOne = createAccount(user.id(), CurrencyCode.USD);
        AccountResponse destinationTwo = createAccount(user.id(), CurrencyCode.USD);
        operationService.deposit(source.id(), money("100.00"));

        List<Object> results = executeConcurrently(
                () -> transferService.transfer(transferRequest(source.id(), destinationOne.id(), "80.00", "concurrent-1")),
                () -> transferService.transfer(transferRequest(source.id(), destinationTwo.id(), "80.00", "concurrent-2"))
        );

        assertThat(results.stream().filter(TransferService.TransferResult.class::isInstance)).hasSize(1);
        assertThat(results.stream().filter(ApiException.class::isInstance)
                .map(ApiException.class::cast).map(ApiException::getCode)).containsExactly("INSUFFICIENT_FUNDS");
        assertBalance(source.id(), "20.0000");
        BigDecimal destinationsTotal = balance(destinationOne.id()).add(balance(destinationTwo.id()));
        assertThat(destinationsTotal).isEqualByComparingTo("80.0000");
    }

    @Test
    void rejectsClosingAccountWithNonZeroBalance() {
        AccountResponse account = createUsdAccount("close@example.com");
        operationService.deposit(account.id(), money("1.00"));

        assertApiCode(() -> accountService.close(account.id()), "NON_ZERO_BALANCE");

        assertThat(accountService.findById(account.id()).status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void paginatesTransactionsNewestFirst() {
        AccountResponse account = createUsdAccount("pagination@example.com");
        for (int i = 1; i <= 5; i++) {
            operationService.deposit(account.id(), money(i + ".00"));
        }

        PageResponse<TransactionResponse> page = transactionService.findByAccount(
                account.id(), PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.content()).hasSize(2);
    }

    @Test
    @WithMockUser(authorities = "deposit:create:any")
    void returnsConsistentValidationErrorWithRequestId() throws Exception {
        String accountId = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/accounts/{accountId}/deposits", accountId)
                        .header("X-Request-ID", "integration-request-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0,\"description\":\"invalid\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Request-ID", "integration-request-id"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.requestId").value("integration-request-id"))
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void openApiDocumentsEveryEndpointGroup() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Virtual Banking API"))
                .andExpect(jsonPath("$.paths['/api/v1/users'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/{userId}/accounts'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/accounts/{accountId}/deposits'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/transfers'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/accounts/{accountId}/transactions'].get").exists());
    }

    private UserResponse createUser(String email) {
        return userService.create(new CreateUserRequest("Test", "Owner", email));
    }

    private AccountResponse createUsdAccount(String email) {
        UserResponse user = createUser(email);
        return createAccount(user.id(), CurrencyCode.USD);
    }

    private AccountResponse createAccount(UUID userId, CurrencyCode currency) {
        return accountService.create(userId, new CreateAccountRequest(AccountType.CHECKING, currency));
    }

    private MoneyOperationRequest money(String amount) {
        return new MoneyOperationRequest(new BigDecimal(amount), "test operation");
    }

    private CreateTransferRequest transferRequest(
            UUID source,
            UUID destination,
            String amount,
            String idempotencyKey
    ) {
        return new CreateTransferRequest(
                source,
                destination,
                new BigDecimal(amount),
                "test transfer",
                idempotencyKey
        );
    }

    private void assertBalance(UUID accountId, String expected) {
        assertThat(balance(accountId)).isEqualByComparingTo(expected);
    }

    private BigDecimal balance(UUID accountId) {
        return accountRepository.findById(accountId).map(BankAccountEntity::getBalance).orElseThrow();
    }

    private void assertApiCode(Callable<?> action, String code) {
        assertThatThrownBy(action::call)
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(code));
    }

    private List<Object> executeConcurrently(Callable<?> first, Callable<?> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Object> firstFuture = executor.submit(concurrentTask(first, ready, start));
            Future<Object> secondFuture = executor.submit(concurrentTask(second, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(firstFuture.get(20, TimeUnit.SECONDS), secondFuture.get(20, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Object> concurrentTask(Callable<?> action, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            try {
                return action.call();
            } catch (ApiException exception) {
                return exception;
            }
        };
    }
}
