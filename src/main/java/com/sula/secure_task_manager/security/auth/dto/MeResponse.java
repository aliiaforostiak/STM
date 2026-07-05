package com.sula.secure_task_manager.security.auth.dto;

import com.sula.secure_task_manager.manager.user.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MeResponse", description = "Current authenticated user")
public record MeResponse(
        @Schema(example = "1")
        Long id,

        @Schema(example = "test@example.com")
        String email,

        Role role
) {
}
