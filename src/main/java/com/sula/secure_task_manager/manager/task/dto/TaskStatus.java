package com.sula.secure_task_manager.manager.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Task lifecycle status")
public enum TaskStatus {

    @Schema(description = "Task is created but work has not started")
    TODO,

    @Schema(description = "Task is in progress")
    IN_PROGRESS,

    @Schema(description = "Task is completed")
    DONE,

    @Schema(description = "Task is cancelled")
    CANCELLED;

    public boolean canMoveTo(TaskStatus nextStatus) {
        return switch (this) {
            case TODO -> nextStatus == IN_PROGRESS || nextStatus == CANCELLED;
            case IN_PROGRESS -> nextStatus == DONE || nextStatus == CANCELLED;
            case DONE, CANCELLED -> false;
        };
    }
}
