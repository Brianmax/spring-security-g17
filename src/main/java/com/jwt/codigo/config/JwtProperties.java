package com.jwt.codigo.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    @NotBlank
    private String issuer;

    @NotBlank
    private String audience;

    @NotNull
    private Duration accessTokenTtl;

    @NotNull
    private Duration refreshTokenTtl;

    @NotNull
    private Resource publicKeyLocation;

    @NotNull
    private Resource privateKeyLocation;

    @NotNull
    private Duration clockSkew;

    @AssertTrue(message = "access-token-ttl must be positive")
    public boolean isAccessTokenTtlValid() {
        return accessTokenTtl == null || (!accessTokenTtl.isZero() && !accessTokenTtl.isNegative());
    }

    @AssertTrue(message = "refresh-token-ttl must be positive")
    public boolean isRefreshTokenTtlValid() {
        return refreshTokenTtl == null || (!refreshTokenTtl.isZero() && !refreshTokenTtl.isNegative());
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public Resource getPublicKeyLocation() {
        return publicKeyLocation;
    }

    public void setPublicKeyLocation(Resource publicKeyLocation) {
        this.publicKeyLocation = publicKeyLocation;
    }

    public Resource getPrivateKeyLocation() {
        return privateKeyLocation;
    }

    public void setPrivateKeyLocation(Resource privateKeyLocation) {
        this.privateKeyLocation = privateKeyLocation;
    }

    public Duration getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(Duration clockSkew) {
        this.clockSkew = clockSkew;
    }
}
