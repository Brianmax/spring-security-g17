package com.jwt.codigo.client;

import com.jwt.codigo.config.DecolectaProperties;
import com.jwt.codigo.dto.exchange.DecolectaExchangeRateResponse;
import com.jwt.codigo.enums.CurrencyCode;
import com.jwt.codigo.exception.ApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Locale;

@Component
public class DecolectaExchangeRateClient implements ExchangeRateClient {

    private static final String AVERAGE_RATE_PATH = "/v1/tipo-cambio/sbs/average";

    private final RestClient restClient;
    private final DecolectaProperties properties;

    public DecolectaExchangeRateClient(
            @Qualifier("decolectaRestClient") RestClient restClient,
            DecolectaProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public ExchangeRateQuote getAverageRate(CurrencyCode currency) {
        if (currency == CurrencyCode.PEN) {
            throw new IllegalArgumentException("Decolecta rates must be requested for USD or EUR");
        }
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "EXCHANGE_RATE_NOT_CONFIGURED",
                    "Cross-currency transfers are unavailable because the exchange-rate provider is not configured"
            );
        }

        DecolectaExchangeRateResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(AVERAGE_RATE_PATH)
                            .queryParam("currency", currency.name())
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(DecolectaExchangeRateResponse.class);
        } catch (ResourceAccessException exception) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "EXCHANGE_RATE_PROVIDER_UNAVAILABLE",
                    "The exchange-rate provider is temporarily unavailable"
            );
        } catch (RestClientResponseException exception) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "EXCHANGE_RATE_PROVIDER_ERROR",
                    "The exchange-rate provider rejected the rate request"
            );
        } catch (RestClientException exception) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "INVALID_EXCHANGE_RATE_RESPONSE",
                    "The exchange-rate provider returned an invalid response"
            );
        }

        validateResponse(response, currency);
        return new ExchangeRateQuote(currency, response.buyPrice(), response.sellPrice(), response.date());
    }

    private void validateResponse(DecolectaExchangeRateResponse response, CurrencyCode requestedCurrency) {
        boolean valid = response != null
                && response.buyPrice() != null
                && response.buyPrice().signum() > 0
                && response.sellPrice() != null
                && response.sellPrice().signum() > 0
                && response.date() != null
                && requestedCurrency.name().equals(normalize(response.baseCurrency()))
                && CurrencyCode.PEN.name().equals(normalize(response.quoteCurrency()));
        if (!valid) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "INVALID_EXCHANGE_RATE_RESPONSE",
                    "The exchange-rate provider returned an invalid response"
            );
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
