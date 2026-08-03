package com.jwt.codigo.client;

import com.jwt.codigo.config.DecolectaProperties;
import com.jwt.codigo.enums.CurrencyCode;
import com.jwt.codigo.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DecolectaExchangeRateClientTest {

    @Test
    void mapsOfficialSbsAverageResponseAndSendsBearerToken() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.decolecta.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DecolectaExchangeRateClient client = new DecolectaExchangeRateClient(
                builder.build(), properties("secret-test-token")
        );
        server.expect(once(), requestTo("https://api.decolecta.test/v1/tipo-cambio/sbs/average?currency=USD"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer secret-test-token"))
                .andRespond(withSuccess("""
                        {
                          "buy_price": "3.540",
                          "sell_price": "3.552",
                          "base_currency": "USD",
                          "quote_currency": "PEN",
                          "date": "2025-07-25"
                        }
                        """, MediaType.APPLICATION_JSON));

        ExchangeRateClient.ExchangeRateQuote result = client.getAverageRate(CurrencyCode.USD);

        assertThat(result.baseCurrency()).isEqualTo(CurrencyCode.USD);
        assertThat(result.buyPrice()).isEqualByComparingTo("3.540");
        assertThat(result.sellPrice()).isEqualByComparingTo("3.552");
        assertThat(result.date()).isEqualTo(LocalDate.of(2025, 7, 25));
        server.verify();
    }

    @Test
    void rejectsCrossCurrencyRequestWhenTokenIsMissing() {
        DecolectaExchangeRateClient client = new DecolectaExchangeRateClient(
                RestClient.create(), properties("")
        );

        assertThatThrownBy(() -> client.getAverageRate(CurrencyCode.USD))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("EXCHANGE_RATE_NOT_CONFIGURED"));
    }

    @Test
    void mapsProviderHttpFailureWithoutExposingItsBody() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.decolecta.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DecolectaExchangeRateClient client = new DecolectaExchangeRateClient(
                builder.build(), properties("bad-token")
        );
        server.expect(once(), requestTo("https://api.decolecta.test/v1/tipo-cambio/sbs/average?currency=EUR"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("sensitive provider response"));

        assertThatThrownBy(() -> client.getAverageRate(CurrencyCode.EUR))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("EXCHANGE_RATE_PROVIDER_ERROR"));
        server.verify();
    }

    private DecolectaProperties properties(String apiKey) {
        return new DecolectaProperties(
                URI.create("https://api.decolecta.test"),
                apiKey,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1)
        );
    }
}
