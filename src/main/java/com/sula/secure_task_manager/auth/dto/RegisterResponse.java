package com.sula.secure_task_manager.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RegisterResponse", description = "Registration result")
public record RegisterResponse(
        @Schema(example = "1")
        Long id,

        @Schema(example = "test@example.com")
        String email
) {
}
