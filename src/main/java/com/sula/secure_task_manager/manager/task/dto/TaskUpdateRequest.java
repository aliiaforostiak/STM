package com.sula.secure_task_manager.manager.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Request body for updating a task")
public record TaskUpdateRequest(
        @Schema(description = "Task title", example = "Implement refresh token flow")
        @Size(max = 200, message = "Task title must be at most 200 characters")
        String title,

        @Schema(description = "Task description", example = "Add refresh endpoint and token rotation")
        @Size(max = 5000, message = "Task description must be at most 5000 characters")
        String description,

        @Schema(description = "Task priority")
        TaskPriority priority,

        @Schema(description = "Task status")
        TaskStatus status,

        @Schema(description = "Assignee user ID", example = "2")
        @Positive(message = "Assignee ID must be positive")
        Long assigneeId,

        @Schema(description = "Task due date", example = "2026-07-06T18:00:00Z")
        Instant dueDate
) {
}
