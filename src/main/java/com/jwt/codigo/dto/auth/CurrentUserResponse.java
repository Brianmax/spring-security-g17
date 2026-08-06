package com.jwt.codigo.dto.auth;

import com.jwt.codigo.enums.UserStatus;

import java.util.Set;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        UserStatus status,
        Set<String> roles,
        Set<String> permissions
) {
}
