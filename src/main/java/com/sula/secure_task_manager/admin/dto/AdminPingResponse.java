package com.sula.secure_task_manager.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminPingResponse", description = "Simple admin access check")
public record AdminPingResponse(
        @Schema(example = "ok")
        String status
) {
}
