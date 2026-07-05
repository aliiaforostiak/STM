package com.sula.secure_task_manager.security.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginResponse", description = "Login result")
public record LoginResponse(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken
) {
}
