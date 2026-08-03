package com.jwt.codigo.service;

import com.jwt.codigo.client.ExchangeRateClient;
import com.jwt.codigo.enums.CurrencyCode;
import com.jwt.codigo.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    private static final LocalDate RATE_DATE = LocalDate.of(2026, 8, 3);

    @Mock
    private ExchangeRateClient exchangeRateClient;

    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        exchangeRateService = new ExchangeRateService(exchangeRateClient);
    }

    @Test
    void keepsSameCurrencyAmountWithoutCallingProvider() {
        ExchangeRateService.CurrencyConversion result = exchangeRateService.convert(
                new BigDecimal("10.0000"), CurrencyCode.USD, CurrencyCode.USD
        );

        assertThat(result.destinationAmount()).isEqualByComparingTo("10.0000");
        assertThat(result.effectiveRate()).isEqualByComparingTo("1.00000000");
        assertThat(result.rateDate()).isNull();
        assertThat(result.provider()).isEqualTo("INTERNAL");
        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void convertsUsdToPenUsingBuyPrice() {
        when(exchangeRateClient.getAverageRate(CurrencyCode.USD)).thenReturn(usdRate(RATE_DATE));

        ExchangeRateService.CurrencyConversion result = exchangeRateService.convert(
                new BigDecimal("10.0000"), CurrencyCode.USD, CurrencyCode.PEN
        );

        assertThat(result.destinationAmount()).isEqualByComparingTo("35.0000");
        assertThat(result.effectiveRate()).isEqualByComparingTo("3.50000000");
        assertThat(result.rateDate()).isEqualTo(RATE_DATE);
    }

    @Test
    void convertsPenToEurUsingSellPrice() {
        when(exchangeRateClient.getAverageRate(CurrencyCode.EUR)).thenReturn(eurRate(RATE_DATE));

        ExchangeRateService.CurrencyConversion result = exchangeRateService.convert(
                new BigDecimal("42.0000"), CurrencyCode.PEN, CurrencyCode.EUR
        );

        assertThat(result.destinationAmount()).isEqualByComparingTo("10.0000");
        assertThat(result.effectiveRate()).isEqualByComparingTo("0.23809524");
    }

    @Test
    void convertsUsdToEurThroughPen() {
        when(exchangeRateClient.getAverageRate(CurrencyCode.USD)).thenReturn(usdRate(RATE_DATE));
        when(exchangeRateClient.getAverageRate(CurrencyCode.EUR)).thenReturn(eurRate(RATE_DATE));

        ExchangeRateService.CurrencyConversion result = exchangeRateService.convert(
                new BigDecimal("10.0000"), CurrencyCode.USD, CurrencyCode.EUR
        );

        assertThat(result.destinationAmount()).isEqualByComparingTo("8.3333");
        assertThat(result.effectiveRate()).isEqualByComparingTo("0.83333000");
    }

    @Test
    void rejectsCrossRateDatesThatDoNotMatch() {
        when(exchangeRateClient.getAverageRate(CurrencyCode.USD)).thenReturn(usdRate(RATE_DATE));
        when(exchangeRateClient.getAverageRate(CurrencyCode.EUR)).thenReturn(eurRate(RATE_DATE.minusDays(1)));

        assertThatThrownBy(() -> exchangeRateService.convert(
                new BigDecimal("10.0000"), CurrencyCode.USD, CurrencyCode.EUR
        )).isInstanceOfSatisfying(ApiException.class,
                exception -> assertThat(exception.getCode()).isEqualTo("EXCHANGE_RATE_DATE_MISMATCH"));
    }

    private ExchangeRateClient.ExchangeRateQuote usdRate(LocalDate date) {
        return new ExchangeRateClient.ExchangeRateQuote(
                CurrencyCode.USD, new BigDecimal("3.5000"), new BigDecimal("3.6000"), date
        );
    }

    private ExchangeRateClient.ExchangeRateQuote eurRate(LocalDate date) {
        return new ExchangeRateClient.ExchangeRateQuote(
                CurrencyCode.EUR, new BigDecimal("4.0000"), new BigDecimal("4.2000"), date
        );
    }
}
