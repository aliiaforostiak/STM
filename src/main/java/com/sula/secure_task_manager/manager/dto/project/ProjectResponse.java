package com.sula.secure_task_manager.manager.dto.project;

import com.sula.secure_task_manager.manager.dto.user.UserShortResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
@Schema(description = "Project response")
public record ProjectResponse(

        @Schema(description = "Project ID", example = "1")
        Long id,

        @Schema(description = "Project name", example = "Secure Task Manager")
        String name,

        @Schema(description = "Project description", example = "Backend project for managing secure tasks")
        String description,

        @Schema(description = "Project owner")
        UserShortResponse owner,

        @Schema(description = "Project creation time", example = "2026-06-30T10:15:30Z")
        Instant createdAt,

        @Schema(description = "Last project update time", example = "2026-06-30T12:45:00Z")
        Instant updatedAt
) {
}
