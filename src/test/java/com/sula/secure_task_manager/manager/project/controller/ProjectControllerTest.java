package com.sula.secure_task_manager.manager.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sula.secure_task_manager.common.exception.base.ApiException;
import com.sula.secure_task_manager.common.exception.base.BadRequestException;
import com.sula.secure_task_manager.common.exception.base.ResourceNotFoundException;
import com.sula.secure_task_manager.manager.project.dto.ProjectCreateRequest;
import com.sula.secure_task_manager.manager.project.dto.ProjectResponse;
import com.sula.secure_task_manager.manager.project.dto.ProjectShortResponse;
import com.sula.secure_task_manager.manager.project.dto.ProjectUpdateRequest;
import com.sula.secure_task_manager.manager.project.service.ProjectService;
import com.sula.secure_task_manager.security.jwt.JwtAuthenticationFilter;
import com.sula.secure_task_manager.manager.user.dto.UserShortResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private ProjectResponse projectResponse() {
        return new ProjectResponse(
                1L,
                "Secure Task Manager",
                "Backend project",
                new UserShortResponse(10L, "Anna"),
                Instant.parse("2026-07-01T10:00:00Z"),
                Instant.parse("2026-07-01T12:00:00Z")
        );
    }

    private ProjectResponse updatedProjectResponse() {
        return new ProjectResponse(
                1L,
                "Secure Task Manager v2",
                "Updated backend project",
                new UserShortResponse(10L, "Anna"),
                Instant.parse("2026-07-01T10:00:00Z"),
                Instant.parse("2026-07-01T13:00:00Z")
        );
    }

    @Nested
    class GetProject {
        @Test
        void getMyProjects_shouldReturnProjects() throws Exception {
            List<ProjectShortResponse> response = List.of(
                    new ProjectShortResponse(1L, "Secure Task Manager"),
                    new ProjectShortResponse(2L, "Learning Platform"));

            when(projectService.getMyProjects()).thenReturn(response);

            mockMvc.perform(get("/api/projects"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Secure Task Manager"))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].name").value("Learning Platform"));

            verify(projectService).getMyProjects();
        }

        @Test
        void getMyProjects_shouldReturnEmptyList_whenUserHasNoProjects() throws Exception {

            when(projectService.getMyProjects()).thenReturn(List.of());

            mockMvc.perform(get("/api/projects"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());

            verify(projectService).getMyProjects();
        }
    }

    @Nested
    class GetProjectById {
        @Test
        void getProjectById_shouldReturnProject() throws Exception {
            when(projectService.getProjectById(1L)).thenReturn(projectResponse());

            mockMvc.perform(get("/api/projects/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Secure Task Manager"))
                    .andExpect(jsonPath("$.description").value("Backend project"))

                    .andExpect(jsonPath("$.owner.id").value(10))
                    .andExpect(jsonPath("$.owner.fullName").value("Anna"));

            verify(projectService, times(1)).getProjectById(1L);
        }

        @Test
        void getProjectById_shouldReturn404_whenProjectNotFound() throws Exception {
            when(projectService.getProjectById(1L)).thenThrow(new ResourceNotFoundException("Project", 1L));

            mockMvc.perform(get("/api/projects/1"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Project with id 1 not found"))
                    .andExpect(jsonPath("$.path").value("/api/projects/1"));

            verify(projectService).getProjectById(1L);
        }
    }

    @Nested
    class CreateProject {
        @Test
        void createProject_shouldReturnCreatedProject() throws Exception {
            ProjectCreateRequest request = new ProjectCreateRequest(
                    "Secure Task Manager",
                    "Backend project"
            );

            when(projectService.createProject(request)).thenReturn(projectResponse());

            mockMvc.perform(post("/api/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))

                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Secure Task Manager"))
                    .andExpect(jsonPath("$.description").value("Backend project"))
                    .andExpect(jsonPath("$.owner.id").value(10))
                    .andExpect(jsonPath("$.owner.fullName").value("Anna"));

            verify(projectService).createProject(request);
        }

        @ParameterizedTest
        @MethodSource("invalidRequests")
        void shouldReturn400_whenFieldHasInvalidValue(ProjectCreateRequest request, String field) throws Exception {

            mockMvc.perform(post("/api/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.details[0].field").value(field));

            verify(projectService, never()).createProject(any());
        }

        static Stream<Arguments> invalidRequests() {
            return Stream.of(
                    Arguments.of(new ProjectCreateRequest("   ", "desc"), "name"),
                    Arguments.of(new ProjectCreateRequest("a".repeat(101), "desc"), "name"),
                    Arguments.of(new ProjectCreateRequest("test", "a".repeat(2001)), "description")
            );
        }

        @Test
        void createProject_shouldReturn409_whenProjectAlreadyExists() throws Exception {
            ProjectCreateRequest request = new ProjectCreateRequest(
                    "Secure Task Management",
                    "Backend project"
            );

            when(projectService.createProject(request)).thenThrow(new ApiException(
                    HttpStatus.CONFLICT,
                    "PROJECT_ALREADY_EXISTS",
                    "Project with this name already exists",
                    "name"
            ) {
            });

            mockMvc.perform(post("/api/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.code").value("PROJECT_ALREADY_EXISTS"))
                    .andExpect(jsonPath("$.message").value("Project with this name already exists"))
                    .andExpect(jsonPath("$.path").value("/api/projects"))
                    .andExpect(jsonPath("$.details[0].field").value("name"));

            verify(projectService).createProject(request);
        }
    }

    @Nested
    class UpdateProject {
        @Test
        void updateProject_shouldReturnUpdatedProject() throws Exception {
            ProjectUpdateRequest request = new ProjectUpdateRequest(
                    "Secure Task Management v2",
                    "Updated backend project"
            );

            when(projectService.updateProject(1L, request)).thenReturn(updatedProjectResponse());

            mockMvc.perform(patch("/api/projects/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Secure Task Manager v2"))
                    .andExpect(jsonPath("$.description").value("Updated backend project"));

            verify(projectService).updateProject(1L, request);
        }

        @Test
        void updateProject_shouldReturnUpdatedProject_whenOnlyNameIsProvided() throws Exception {
            ProjectUpdateRequest request = new ProjectUpdateRequest(
                    "Secure Task Manager v2",
                    null
            );
            ProjectResponse response = new ProjectResponse(
                    1L,
                    "Secure Task Manager v2",
                    null,
                    new UserShortResponse(10L, "Anna"),
                    Instant.parse("2026-07-01T10:00:00Z"),
                    Instant.parse("2026-07-01T13:00:00Z")
            );

            when(projectService.updateProject(1L, request)).thenReturn(response);

            mockMvc.perform(patch("/api/projects/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Secure Task Manager v2"))
                    .andExpect(jsonPath("$.description").isEmpty());

            verify(projectService).updateProject(1L, request);
        }

        @Test
        void updateProject_shouldReturnUpdatedProject_whenOnlyDescriptionIsProvided() throws Exception {
            ProjectUpdateRequest request = new ProjectUpdateRequest(
                    null,
                    "Updated backend project"
            );

            ProjectResponse response = new ProjectResponse(
                    1L,
                    "Secure Task Manager",
                    "Updated backend project",
                    new UserShortResponse(10L, "Anna"),
                    Instant.parse("2026-07-01T10:00:00Z"),
                    Instant.parse("2026-07-01T13:00:00Z")
            );

            when(projectService.updateProject(1L, request)).thenReturn(response);

            mockMvc.perform(patch("/api/projects/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Secure Task Manager"))
                    .andExpect(jsonPath("$.description").value("Updated backend project"));

            verify(projectService).updateProject(1L, request);
        }

        @Test
        void updateProject_shouldReturn400_whenRequestIsEmpty() throws Exception {
            ProjectUpdateRequest request = new ProjectUpdateRequest(
                    null,
                    null
            );

            when(projectService.updateProject(1L, request))
                    .thenThrow(new BadRequestException("At least one field must be provided"));

            mockMvc.perform(patch("/api/projects/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.message").value("At least one field must be provided"))
                    .andExpect(jsonPath("$.path").value("/api/projects/1"))
                    .andExpect(jsonPath("$.details[0].field").value("request"));

            verify(projectService).updateProject(1L, request);
        }

        @Test
        void updateProject_shouldReturn404_whenProjectNotFound() throws Exception {
            ProjectUpdateRequest request = new ProjectUpdateRequest(
                    "Secure Task Manager v2",
                    "Updated backend project"
            );

            when(projectService.updateProject(1L, request))
                    .thenThrow(new ResourceNotFoundException("Project", 1L));

            mockMvc.perform(patch("/api/projects/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Project with id 1 not found"))
                    .andExpect(jsonPath("$.path").value("/api/projects/1"));

            verify(projectService).updateProject(1L, request);
        }

        @Test
        void updateProject_shouldReturn400_whenNameIsTooLong() throws Exception {
            ProjectUpdateRequest request = new ProjectUpdateRequest(
                    "a".repeat(101),
                    "Updated backend project"
            );

            mockMvc.perform(patch("/api/projects/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.details[0].field").value("name"));

            verify(projectService, never()).updateProject(any(), any());
        }

        @Test
        void updateProject_shouldReturn400_whenDescriptionIsTooLong() throws Exception {
            ProjectUpdateRequest request = new ProjectUpdateRequest(
                    "Secure Task Manager v2",
                    "a".repeat(2001)
            );

            mockMvc.perform(patch("/api/projects/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.details[0].field").value("description"));

            verify(projectService, never()).updateProject(any(), any());
        }
    }

    @Nested
    class DeleteProject {
        @Test
        void deleteProject_shouldReturn404_whenProjectNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Project", 1L))
                    .when(projectService)
                    .deleteProject(1L);

            mockMvc.perform(delete("/api/projects/1"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Project with id 1 not found"))
                    .andExpect(jsonPath("$.path").value("/api/projects/1"));

            verify(projectService).deleteProject(1L);
        }

        @Test
        void deleteProject_shouldReturnNoContent() throws Exception {
            mockMvc.perform(delete("/api/projects/1"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(projectService).deleteProject(1L);
        }
    }
}
