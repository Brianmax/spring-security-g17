package com.jwt.codigo.dto.auth;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record ReplaceRolesRequest(
        @NotEmpty @Size(max = 10) Set<@NotNull String> roles
) {
}
