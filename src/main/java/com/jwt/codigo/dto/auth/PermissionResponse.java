package com.jwt.codigo.dto.auth;

import java.util.UUID;

public record PermissionResponse(
        UUID id,
        String code,
        String description
) {
}
