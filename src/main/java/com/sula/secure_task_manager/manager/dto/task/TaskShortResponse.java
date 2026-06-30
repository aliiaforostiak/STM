package com.sula.secure_task_manager.manager.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Compact task response")
public record TaskShortResponse(
        @Schema(description = "Task ID", example = "10")
        Long id,

        @Schema(description = "Task title", example = "Implement JWT login")
        String title,

        @Schema(description = "Task status")
        TaskStatus status,

        @Schema(description = "Task priority")
        TaskPriority priority
) {
}
