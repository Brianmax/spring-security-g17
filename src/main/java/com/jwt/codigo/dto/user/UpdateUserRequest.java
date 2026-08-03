package com.jwt.codigo.dto.user;

import com.jwt.codigo.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Complete replacement of editable user fields")
public record UpdateUserRequest(
        @NotBlank @Size(max = 100) @Schema(example = "Ada") String firstName,
        @NotBlank @Size(max = 100) @Schema(example = "Byron") String lastName,
        @NotBlank @Email @Size(max = 320) @Schema(example = "ada.byron@example.com") String email,
        @NotNull @Schema(example = "ACTIVE") UserStatus status
) {
}
