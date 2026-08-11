package com.jwt.codigo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;
import java.rmi.server.ExportException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(csrf->csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt()

    }

    @Bean
    PasswordEncoder passwordEncoder(@Value("${security.password.bcrypt-strength}") int strength) {
        if(strength < 10 || strength > 16) {
            throw new IllegalArgumentException("security.password.bcrypt-strength debe de estar entre 10 y 16");
        }
        return new BCryptPasswordEncoder(strength);
    }

    @Bean
    JwtEncoder jwtEncoder() {
        // configuracion de encoder
        return null;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return null;
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
