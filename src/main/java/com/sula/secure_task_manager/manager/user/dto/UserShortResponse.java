package com.sula.secure_task_manager.manager.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Compact user response")
public record UserShortResponse(
        @Schema(description = "User ID", example = "2")
        Long id,

        @Schema(description = "User full name", example = "Alice Johnson")
        String fullName
) {
}
