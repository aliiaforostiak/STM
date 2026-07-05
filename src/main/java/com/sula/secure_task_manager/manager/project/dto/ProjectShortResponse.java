package com.sula.secure_task_manager.manager.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Compact project response")
public record ProjectShortResponse(
        @Schema(description = "Project ID", example = "1")
        Long id,

        @Schema(description = "Project name", example = "Secure Task Manager")
        String name
) {
}
