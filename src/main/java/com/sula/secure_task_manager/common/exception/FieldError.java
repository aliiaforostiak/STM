package com.sula.secure_task_manager.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FieldError", description = "Single field validation error")
public record FieldError(
        @Schema(example = "email")
        String field,

        @Schema(example = "must be a well-formed email address")
        String message
) {
}
