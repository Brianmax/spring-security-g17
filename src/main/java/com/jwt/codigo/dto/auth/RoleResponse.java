package com.jwt.codigo.dto.auth;

import java.util.Set;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String code,
        String description,
        Set<String> permissions
) {
}
