package com.jwt.codigo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jwt.codigo.exception.ApiErrorResponse;
import com.jwt.codigo.filter.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

final class SecurityErrorWriter {
    private SecurityErrorWriter() {}

    static void write(ObjectMapper mapper, HttpServletRequest request, HttpServletResponse response,
                      int status, String code, String message) throws IOException {
        Object requestId = request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), new ApiErrorResponse(Instant.now(), status, code, message,
                request.getRequestURI(), requestId == null ? "unknown" : requestId.toString(), Map.of()));
    }
}
