package com.sula.secure_task_manager.manager.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Task priority level")
public enum TaskPriority {

    @Schema(description = "Low priority task")
    LOW,

    @Schema(description = "Medium priority task")
    MEDIUM,

    @Schema(description = "High priority task")
    HIGH,

    @Schema(description = "Critical priority task")
    CRITICAL

}
