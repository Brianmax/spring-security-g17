package com.jwt.codigo.config;

import com.jwt.codigo.security.JwtUserAuthenticationConverter;
import com.jwt.codigo.security.RestAccessDeniedHandler;
import com.jwt.codigo.security.RestAuthenticationEntryPoint;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import com.nimbusds.jose.jwk.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            JwtUserAuthenticationConverter authConverter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        return httpSecurity
                .csrf(csrf->csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                authConverter
                        ))
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder(@Value("${security.password.bcrypt-strength}") int strength) {
        if(strength < 10 || strength > 16) {
            throw new IllegalArgumentException("security.password.bcrypt-strength debe de estar entre 10 y 16");
        }
        return new BCryptPasswordEncoder(strength);
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties properties) {
        RSAPublicKey publicKey = readPublicKey(properties);
        RSAPrivateKey privateKey = readPrivateKey(properties);
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .build();
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(source);
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(readPublicKey(properties))
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        JwtTimestampValidator timestamps = new JwtTimestampValidator(properties.getClockSkew());
        JwtIssuerValidator issuer = new JwtIssuerValidator(properties.getIssuer());

        OAuth2TokenValidator<Jwt> audience = jwt ->
                jwt.getAudience().contains(properties.getAudience())
                ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("" +
                        "Invalid token",
                        "El audience no existe o no coincide",
                        null));

        OAuth2TokenValidator<Jwt> requiredTimes = jwt -> {
            Instant latestIssue = Instant.now().plus(properties.getClockSkew());
            boolean valid = jwt.getIssuedAt() != null
                    && jwt.getNotBefore() != null
                    && jwt.getExpiresAt() != null
                    && !jwt.getIssuedAt().isAfter(latestIssue);
            return valid ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                            "invalid token",
                    "tiempos invalidos",
                    null
            ));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestamps, issuer, audience, requiredTimes
        ));
        return decoder;
    }

    private RSAPublicKey readPublicKey(JwtProperties jwtProperties) {
        try {
            byte[] bytes = decodePem(jwtProperties.getPublicKeyLocation()
                    .getContentAsString(StandardCharsets.US_ASCII), "PUBLIC KEY");
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar el JWT public key");
        }
    }

    private RSAPrivateKey readPrivateKey(JwtProperties jwtProperties) {
        try {
            byte[] bytes = decodePem(jwtProperties.getPrivateKeyLocation()
                    .getContentAsString(StandardCharsets.US_ASCII), "PRIVATE KEY");
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo cargar el JWT private key");
        }

    }

    private byte[] decodePem(String pem, String label) {
        String base64 = pem
                .replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }

}
