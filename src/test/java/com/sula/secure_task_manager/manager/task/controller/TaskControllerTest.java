package com.sula.secure_task_manager.manager.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sula.secure_task_manager.common.exception.base.BadRequestException;
import com.sula.secure_task_manager.common.exception.base.ResourceNotFoundException;
import com.sula.secure_task_manager.manager.project.dto.ProjectShortResponse;
import com.sula.secure_task_manager.manager.task.dto.*;
import com.sula.secure_task_manager.manager.task.service.TaskService;
import com.sula.secure_task_manager.manager.user.dto.UserShortResponse;
import com.sula.secure_task_manager.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private TaskResponse taskResponse() {
        return new TaskResponse(
                10L,
                "Implement JWT login",
                "Add access token generation and login endpoint",
                TaskPriority.HIGH,
                TaskStatus.TODO,
                new ProjectShortResponse(1L, "Secure Task Manager"),
                new UserShortResponse(7L, "Anna"),
                new UserShortResponse(8L, "Max"),
                Instant.parse("2026-07-10T10:00:00Z"),
                null,
                Instant.parse("2026-07-01T10:00:00Z"),
                Instant.parse("2026-07-01T10:00:00Z")
        );
    }

    @Nested
    class GetTask {

        @Test
        void getTaskById_shouldReturnTask() throws Exception {
            when(taskService.getTaskById(10L)).thenReturn(taskResponse());

            mockMvc.perform(get("/api/tasks/10"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.title").value("Implement JWT login"))
                    .andExpect(jsonPath("$.priority").value("HIGH"))
                    .andExpect(jsonPath("$.status").value("TODO"))
                    .andExpect(jsonPath("$.project.id").value(1))
                    .andExpect(jsonPath("$.creator.id").value(7))
                    .andExpect(jsonPath("$.assignee.id").value(8));

            verify(taskService).getTaskById(10L);
        }

        @Test
        void getTaskById_shouldReturn404_whenTaskNotFound() throws Exception {
            when(taskService.getTaskById(10L))
                    .thenThrow(new ResourceNotFoundException("Task", 10L));

            mockMvc.perform(get("/api/tasks/10"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Task with id 10 not found"));

            verify(taskService).getTaskById(10L);
        }
    }

    @Nested
    class GetProjectTasks {

        @Test
        void getProjectTasks_shouldReturnTasks() throws Exception {
            List<TaskShortResponse> response = List.of(
                    new TaskShortResponse(10L, "Implement JWT login", TaskStatus.TODO, TaskPriority.HIGH),
                    new TaskShortResponse(11L, "Add refresh flow", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM)
            );

            when(taskService.getProjectTasks(1L)).thenReturn(response);

            mockMvc.perform(get("/api/projects/1/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(10))
                    .andExpect(jsonPath("$[0].title").value("Implement JWT login"))
                    .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"));

            verify(taskService).getProjectTasks(1L);
        }

        @Test
        void getProjectTasks_shouldReturnEmptyList_whenProjectHasNoTasks() throws Exception {
            when(taskService.getProjectTasks(1L)).thenReturn(List.of());

            mockMvc.perform(get("/api/projects/1/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());

            verify(taskService).getProjectTasks(1L);
        }
    }

    @Nested
    class CreateTask {

        @Test
        void createTask_shouldReturnCreatedTask() throws Exception {
            TaskCreateRequest request = new TaskCreateRequest(
                    "Implement JWT login",
                    "Add access token generation and login endpoint",
                    1L,
                    8L,
                    TaskPriority.HIGH,
                    Instant.parse("2026-07-10T10:00:00Z")
            );

            when(taskService.createTask(request)).thenReturn(taskResponse());

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.title").value("Implement JWT login"))
                    .andExpect(jsonPath("$.priority").value("HIGH"));

            verify(taskService).createTask(request);
        }

        @Test
        void createTask_shouldReturn400_whenTitleIsBlank() throws Exception {
            TaskCreateRequest request = new TaskCreateRequest(
                    "   ",
                    "Add access token generation and login endpoint",
                    1L,
                    8L,
                    TaskPriority.HIGH,
                    Instant.parse("2026-07-10T10:00:00Z")
            );

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.details[0].field").value("title"));

            verify(taskService, never()).createTask(any());
        }

        @Test
        void createTask_shouldReturn400_whenProjectIdIsMissing() throws Exception {
            TaskCreateRequest request = new TaskCreateRequest(
                    "Implement JWT login",
                    "Add access token generation and login endpoint",
                    null,
                    8L,
                    TaskPriority.HIGH,
                    Instant.parse("2026-07-10T10:00:00Z")
            );

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.details[0].field").value("projectId"));

            verify(taskService, never()).createTask(any());
        }

        @Test
        void createTask_shouldReturn404_whenProjectNotFound() throws Exception {
            TaskCreateRequest request = new TaskCreateRequest(
                    "Implement JWT login",
                    "Add access token generation and login endpoint",
                    1L,
                    8L,
                    TaskPriority.HIGH,
                    Instant.parse("2026-07-10T10:00:00Z")
            );

            when(taskService.createTask(request))
                    .thenThrow(new ResourceNotFoundException("Project", 1L));

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Project with id 1 not found"));

            verify(taskService).createTask(request);
        }
    }

    @Nested
    class UpdateTask {

        @Test
        void updateTask_shouldReturnUpdatedTask() throws Exception {
            TaskUpdateRequest request = new TaskUpdateRequest(
                    "Implement refresh token flow",
                    "Add refresh endpoint and token rotation",
                    TaskPriority.CRITICAL,
                    TaskStatus.IN_PROGRESS,
                    8L,
                    Instant.parse("2026-07-11T10:00:00Z")
            );

            TaskResponse response = new TaskResponse(
                    10L,
                    "Implement refresh token flow",
                    "Add refresh endpoint and token rotation",
                    TaskPriority.CRITICAL,
                    TaskStatus.IN_PROGRESS,
                    new ProjectShortResponse(1L, "Secure Task Manager"),
                    new UserShortResponse(7L, "Anna"),
                    new UserShortResponse(8L, "Max"),
                    Instant.parse("2026-07-11T10:00:00Z"),
                    null,
                    Instant.parse("2026-07-01T10:00:00Z"),
                    Instant.parse("2026-07-02T10:00:00Z")
            );

            when(taskService.updateTask(10L, request)).thenReturn(response);

            mockMvc.perform(patch("/api/tasks/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.title").value("Implement refresh token flow"))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.priority").value("CRITICAL"));

            verify(taskService).updateTask(10L, request);
        }

        @Test
        void updateTask_shouldReturn400_whenTitleIsTooLong() throws Exception {
            TaskUpdateRequest request = new TaskUpdateRequest(
                    "a".repeat(201),
                    "desc",
                    TaskPriority.HIGH,
                    TaskStatus.TODO,
                    8L,
                    Instant.parse("2026-07-11T10:00:00Z")
            );

            mockMvc.perform(patch("/api/tasks/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.details[0].field").value("title"));

            verify(taskService, never()).updateTask(any(), any());
        }

        @Test
        void updateTask_shouldReturn400_whenNoFieldsProvided() throws Exception {
            TaskUpdateRequest request = new TaskUpdateRequest(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            when(taskService.updateTask(10L, request))
                    .thenThrow(new BadRequestException("At least one field must be provided"));

            mockMvc.perform(patch("/api/tasks/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.message").value("At least one field must be provided"));

            verify(taskService).updateTask(10L, request);
        }

        @Test
        void updateTask_shouldReturn404_whenTaskNotFound() throws Exception {
            TaskUpdateRequest request = new TaskUpdateRequest(
                    "Implement refresh token flow",
                    null,
                    null,
                    null,
                    null,
                    null
            );

            when(taskService.updateTask(10L, request))
                    .thenThrow(new ResourceNotFoundException("Task", 10L));

            mockMvc.perform(patch("/api/tasks/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Task with id 10 not found"));

            verify(taskService).updateTask(10L, request);
        }

        @Test
        void updateTask_shouldReturn403_whenUserHasNoAccess() throws Exception {
            TaskUpdateRequest request = new TaskUpdateRequest(
                    "Implement refresh token flow",
                    null,
                    null,
                    null,
                    null,
                    null
            );

            when(taskService.updateTask(10L, request))
                    .thenThrow(new AccessDeniedException("You do not have access to this task"));

            mockMvc.perform(patch("/api/tasks/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                    .andExpect(jsonPath("$.message").value("You do not have access to this task"));

            verify(taskService).updateTask(10L, request);
        }
    }

    @Nested
    class DeleteTask {

        @Test
        void deleteTask_shouldReturnNoContent() throws Exception {
            mockMvc.perform(delete("/api/tasks/10"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(taskService).deleteTask(10L);
        }

        @Test
        void deleteTask_shouldReturn404_whenTaskNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Task", 10L))
                    .when(taskService)
                    .deleteTask(10L);

            mockMvc.perform(delete("/api/tasks/10"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Task with id 10 not found"));

            verify(taskService).deleteTask(10L);
        }
    }
}
