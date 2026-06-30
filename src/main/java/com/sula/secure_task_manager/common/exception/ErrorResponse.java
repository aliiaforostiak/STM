package com.sula.secure_task_manager.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "ErrorResponse", description = "Standard API error response")
public record ErrorResponse(
        @Schema(example = "2026-06-30T10:15:30Z")
        Instant timestamp,

        @Schema(example = "400")
        int status,

        @Schema(example = "validation_error")
        String code,

        @Schema(example = "Validation failed")
        String message,

        @Schema(example = "/api/auth/register")
        String path,

        @Schema(description = "Field-specific validation details")
        List<FieldError> details
) {
}
