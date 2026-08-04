package com.jwt.codigo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
        return httpSecurity.build();
    }

    @Bean
    PasswordEncoder passwordEncoder(int strenght) {
        return new BCryptPasswordEncoder(strenght);
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return null;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return null;
    }

    private RSAPublicKey readPublicKey() {
        return null;
    }

    private RSAPrivateKey readPrivateKey() {
        return null;
    }


}
