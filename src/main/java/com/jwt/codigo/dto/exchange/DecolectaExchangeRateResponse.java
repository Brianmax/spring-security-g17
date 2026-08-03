package com.jwt.codigo.dto.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DecolectaExchangeRateResponse(
        @JsonProperty("buy_price") BigDecimal buyPrice,
        @JsonProperty("sell_price") BigDecimal sellPrice,
        @JsonProperty("base_currency") String baseCurrency,
        @JsonProperty("quote_currency") String quoteCurrency,
        LocalDate date
) {
}
