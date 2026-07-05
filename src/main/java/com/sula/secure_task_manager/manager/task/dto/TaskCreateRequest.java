package com.sula.secure_task_manager.manager.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Request body for creating a task")
public record TaskCreateRequest (
        @Schema(description = "Task title", example = "Implement JWT login")
        @NotBlank(message = "Task title is required")
        @Size(max = 200, message = "Task title must be at most 200 characters")
        String title,

        @Schema(description = "Task description", example = "Add access token generation and login endpoint")
        @Size(max = 5000, message = "Task description must be at most 5000 characters")
        String description,

        @Schema(description = "Project ID", example = "1")
        @NotNull(message = "Project ID is required")
        @Positive(message = "Project ID must be positive")
        Long projectId,

        @Schema(description = "Assignee user ID", example = "2")
        @Positive(message = "Assignee ID must be positive")
        Long assigneeId,

        @Schema(description = "Task priority")
        @NotNull(message = "Task priority is required")
        TaskPriority priority,

        @Schema(description = "Task due date", example = "2026-07-05T18:00:00Z")
        Instant dueDate
) {
}
