package com.sula.secure_task_manager.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RegisterRequest", description = "Registration payload")
public record RegisterRequest(
        @Schema(example = "test@example.com")
        @Email
        @NotBlank
        String email,

        @Schema(example = "password123")
        @NotBlank
        @Size(min = 8, max = 100)
        String password) {
}
