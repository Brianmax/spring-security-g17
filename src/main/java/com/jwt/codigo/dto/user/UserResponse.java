package com.jwt.codigo.dto.user;

import com.jwt.codigo.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Virtual banking user")
public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
