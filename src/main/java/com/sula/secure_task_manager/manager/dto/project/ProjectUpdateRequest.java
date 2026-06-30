package com.sula.secure_task_manager.manager.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for updating a project")
public record ProjectUpdateRequest(
        @Schema(description = "Project name", example = "Secure Task Manager API")
        @Size(max = 100, message = "Project name must be at most 100 characters")
        String name,

        @Schema(description = "Project description", example = "Updated backend project for managing secure tasks")
        @Size(max = 2000, message = "Project description must be at most 2000 characters")
        String description
) {
}
