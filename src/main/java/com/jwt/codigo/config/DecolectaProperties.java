package com.jwt.codigo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "integrations.decolecta")
public record DecolectaProperties(
        URI baseUrl,
        String apiKey,
        Duration connectTimeout,
        Duration readTimeout
) {
    public DecolectaProperties {
        baseUrl = baseUrl == null ? URI.create("https://api.decolecta.com") : baseUrl;
        apiKey = apiKey == null ? "" : apiKey.trim();
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(5) : readTimeout;
    }
}
