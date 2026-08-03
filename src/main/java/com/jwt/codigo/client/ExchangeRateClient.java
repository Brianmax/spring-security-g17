package com.jwt.codigo.client;

import com.jwt.codigo.enums.CurrencyCode;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ExchangeRateClient {

    ExchangeRateQuote getAverageRate(CurrencyCode currency);

    record ExchangeRateQuote(
            CurrencyCode baseCurrency,
            BigDecimal buyPrice,
            BigDecimal sellPrice,
            LocalDate date
    ) {
    }
}
