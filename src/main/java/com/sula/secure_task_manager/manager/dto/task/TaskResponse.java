package com.sula.secure_task_manager.manager.dto.task;

import com.sula.secure_task_manager.manager.dto.project.ProjectShortResponse;
import com.sula.secure_task_manager.manager.dto.user.UserShortResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Full task response")
public record TaskResponse (
        @Schema(description = "Task ID", example = "10")
        Long id,

        @Schema(description = "Task title", example = "Implement JWT login")
        String title,

        @Schema(description = "Task description", example = "Add access token generation and login endpoint")
        String description,

        @Schema(description = "Task priority")
        TaskPriority priority,

        @Schema(description = "Task status")
        TaskStatus status,

        @Schema(description = "Project summary")
        ProjectShortResponse project,

        @Schema(description = "Task creator")
        UserShortResponse creator,

        @Schema(description = "Task assignee")
        UserShortResponse assignee,

        @Schema(description = "Task due date", example = "2026-07-05T18:00:00Z")
        Instant dueDate,

        @Schema(description = "Task completion time", example = "2026-07-04T15:30:00Z")
        Instant completedAt,

        @Schema(description = "Task creation time", example = "2026-06-30T10:15:30Z")
        Instant createdAt,

        @Schema(description = "Last task update time", example = "2026-06-30T12:45:00Z")
        Instant updatedAt
){
}
