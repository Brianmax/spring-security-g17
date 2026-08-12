package com.jwt.codigo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        String details = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
        String code = details.contains("expired") ? "ACCESS_TOKEN_EXPIRED" : "INVALID_ACCESS_TOKEN";
        SecurityErrorWriter.write(objectMapper, request, response, 401, code, "Authentication is required");
    }
}
