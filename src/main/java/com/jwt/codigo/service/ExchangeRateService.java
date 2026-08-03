package com.jwt.codigo.service;

import com.jwt.codigo.client.ExchangeRateClient;
import com.jwt.codigo.enums.CurrencyCode;
import com.jwt.codigo.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class ExchangeRateService {

    public static final String INTERNAL_PROVIDER = "INTERNAL";
    public static final String DECOLECTA_PROVIDER = "DECOLECTA_SBS_AVERAGE";
    private static final int MONEY_SCALE = 4;
    private static final int RATE_SCALE = 8;

    private final ExchangeRateClient exchangeRateClient;

    public ExchangeRateService(ExchangeRateClient exchangeRateClient) {
        this.exchangeRateClient = exchangeRateClient;
    }

    public CurrencyConversion convert(
            BigDecimal sourceAmount,
            CurrencyCode sourceCurrency,
            CurrencyCode destinationCurrency
    ) {
        if (sourceCurrency == destinationCurrency) {
            return new CurrencyConversion(
                    sourceAmount,
                    BigDecimal.ONE.setScale(RATE_SCALE),
                    null,
                    INTERNAL_PROVIDER
            );
        }

        BigDecimal destinationAmount;
        LocalDate rateDate;
        if (destinationCurrency == CurrencyCode.PEN) {
            ExchangeRateClient.ExchangeRateQuote sourceRate = exchangeRateClient.getAverageRate(sourceCurrency);
            destinationAmount = sourceAmount.multiply(sourceRate.buyPrice());
            rateDate = sourceRate.date();
        } else if (sourceCurrency == CurrencyCode.PEN) {
            ExchangeRateClient.ExchangeRateQuote destinationRate = exchangeRateClient.getAverageRate(destinationCurrency);
            destinationAmount = sourceAmount.divide(destinationRate.sellPrice(), MONEY_SCALE, RoundingMode.HALF_EVEN);
            rateDate = destinationRate.date();
        } else {
            ExchangeRateClient.ExchangeRateQuote sourceRate = exchangeRateClient.getAverageRate(sourceCurrency);
            ExchangeRateClient.ExchangeRateQuote destinationRate = exchangeRateClient.getAverageRate(destinationCurrency);
            if (!sourceRate.date().equals(destinationRate.date())) {
                throw new ApiException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "EXCHANGE_RATE_DATE_MISMATCH",
                        "The exchange-rate provider returned rates from different dates"
                );
            }
            destinationAmount = sourceAmount
                    .multiply(sourceRate.buyPrice())
                    .divide(destinationRate.sellPrice(), MONEY_SCALE, RoundingMode.HALF_EVEN);
            rateDate = sourceRate.date();
        }

        destinationAmount = destinationAmount.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
        if (destinationAmount.signum() <= 0) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "CONVERTED_AMOUNT_TOO_SMALL",
                    "The converted amount is too small for the destination currency"
            );
        }
        BigDecimal effectiveRate = destinationAmount.divide(sourceAmount, RATE_SCALE, RoundingMode.HALF_EVEN);
        return new CurrencyConversion(destinationAmount, effectiveRate, rateDate, DECOLECTA_PROVIDER);
    }

    public record CurrencyConversion(
            BigDecimal destinationAmount,
            BigDecimal effectiveRate,
            LocalDate rateDate,
            String provider
    ) {
    }
}
