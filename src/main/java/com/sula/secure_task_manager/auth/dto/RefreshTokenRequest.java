package com.sula.secure_task_manager.auth.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RefreshTokenRequest", description = "Refresh token payload")
public record RefreshTokenRequest(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank
        String refreshToken
) {
}
