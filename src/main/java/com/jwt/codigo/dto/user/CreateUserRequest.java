package com.jwt.codigo.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "New virtual banking user")
public record CreateUserRequest(
        @NotBlank @Size(max = 100) @Schema(example = "Ada") String firstName,
        @NotBlank @Size(max = 100) @Schema(example = "Lovelace") String lastName,
        @NotBlank @Email @Size(max = 320) @Schema(example = "ada@example.com") String email
) {
}
