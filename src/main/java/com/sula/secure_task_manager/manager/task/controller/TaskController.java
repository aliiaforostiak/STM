package com.sula.secure_task_manager.manager.task.controller;

import com.sula.secure_task_manager.common.dto.PageResponse;
import com.sula.secure_task_manager.common.exception.response.ErrorResponse;
import com.sula.secure_task_manager.manager.task.dto.TaskCreateRequest;
import com.sula.secure_task_manager.manager.task.dto.TaskResponse;
import com.sula.secure_task_manager.manager.task.dto.TaskShortResponse;
import com.sula.secure_task_manager.manager.task.dto.TaskUpdateRequest;
import com.sula.secure_task_manager.manager.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management API")
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/api/tasks/{id}")
    @Operation(summary = "Get task by ID", description = "Returns detailed information about a task by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task returned successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public TaskResponse getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    @GetMapping("/api/projects/{projectId}/tasks")
    @Operation(summary = "Get project tasks", description = "Returns tasks that belong to the specified project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tasks returned successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<TaskShortResponse> getProjectTasks(@PathVariable Long projectId) {
        return taskService.getProjectTasks(projectId);
    }

    @GetMapping("/api/projects/{projectId}/tasks/paged")
    @Operation(summary = "Get project tasks page", description = "Returns a paginated list of tasks that belong to the specified project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tasks page returned successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PageResponse<TaskShortResponse> getProjectTasksPage(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return taskService.getProjectTasksPage(projectId, page, size);
    }

    @PostMapping("/api/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create task", description = "Creates a new task in the specified project")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Task created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Related resource not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public TaskResponse createTask(@Valid @RequestBody TaskCreateRequest request) {
        return taskService.createTask(request);
    }

    @PatchMapping("/api/tasks/{id}")
    @Operation(summary = "Update task", description = "Partially updates task fields such as title, description, status or assignee")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public TaskResponse updateTask(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        return taskService.updateTask(id, request);
    }

    @DeleteMapping("/api/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete task", description = "Deletes a task by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
    }
}
