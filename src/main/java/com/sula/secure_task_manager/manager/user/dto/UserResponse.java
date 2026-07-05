package com.sula.secure_task_manager.manager.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Full user response")
public record UserResponse(
        @Schema(description = "User ID", example = "2")
        Long id,

        @Schema(description = "User email", example = "alice@example.com")
        String email,

        @Schema(description = "User full name", example = "Alice Johnson")
        String fullName,

        @Schema(description = "Whether the user is enabled", example = "true")
        boolean enabled,

        @Schema(description = "User creation time", example = "2026-06-30T10:15:30Z")
        Instant createdAt
) {
}
