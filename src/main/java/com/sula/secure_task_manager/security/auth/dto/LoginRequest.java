package com.sula.secure_task_manager.security.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginRequest", description = "Login payload")
public record LoginRequest(
        @Schema(example = "test@example.com")
        @Email
        @NotBlank
        String email,

        @Schema(example = "password123")
        @NotBlank
        String password
) {
}
