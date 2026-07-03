package com.sula.secure_task_manager.manager.service;

import com.sula.secure_task_manager.common.exception.BadRequestException;
import com.sula.secure_task_manager.common.exception.ProjectAlreadyExistsException;
import com.sula.secure_task_manager.common.exception.ResourceNotFoundException;
import com.sula.secure_task_manager.manager.dto.project.ProjectCreateRequest;
import com.sula.secure_task_manager.manager.dto.project.ProjectResponse;
import com.sula.secure_task_manager.manager.dto.project.ProjectShortResponse;
import com.sula.secure_task_manager.manager.dto.project.ProjectUpdateRequest;
import com.sula.secure_task_manager.manager.entity.Project;
import com.sula.secure_task_manager.manager.repository.ProjectRepository;
import com.sula.secure_task_manager.user.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProjectService projectService;

    @Nested
    class GetMyProjects {
        @Test
        void shouldReturnUserProjects() {
            Long userId = 1L;
            Project first = Project.builder()
                    .id(1L)
                    .name("Secure Task Manager")
                    .ownerId(userId)
                    .build();
            Project second = Project.builder()
                    .id(2L)
                    .name("Notification Service")
                    .ownerId(userId)
                    .build();

            List<Project> projects = List.of(first, second);

            when(userService.getCurrentUserId()).thenReturn(userId);
            when(projectRepository.findAllByOwnerId(userId)).thenReturn(projects);

            List<ProjectShortResponse> result = projectService.getMyProjects();

            assertThat(result)
                    .extracting(ProjectShortResponse::id, ProjectShortResponse::name)
                    .containsExactly(
                            tuple(1L, "Secure Task Manager"),
                            tuple(2L, "Notification Service"));

            verify(userService).getCurrentUserId();
            verify(projectRepository).findAllByOwnerId(userId);
            verifyNoMoreInteractions(projectRepository);
        }

        @Test
        void shouldReturnEmptyList_whenUserHasNoProjects() {
            Long userId = 1L;
            when(userService.getCurrentUserId()).thenReturn(userId);
            when(projectRepository.findAllByOwnerId(userId)).thenReturn(List.of());

            List<ProjectShortResponse> result = projectService.getMyProjects();

            assertThat(result).isEmpty();

            verify(userService).getCurrentUserId();
            verify(projectRepository).findAllByOwnerId(userId);
            verifyNoMoreInteractions(projectRepository);
        }
    }

    @Nested
    class GetMyProjectById {

        @Test
        void shouldReturnDetailedProjectById() {
            Long projectId = 1L;
            Project project = Project.builder()
                    .id(projectId)
                    .name("Secure task Management")
                    .description("Backend project")
                    .ownerId(10L)
                    .build();

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

            ProjectResponse result = projectService.getProjectById(projectId);

            assertThat(result.id()).isEqualTo(projectId);
            assertThat(result.name()).isEqualTo("Secure task Management");
            assertThat(result.description()).isEqualTo("Backend project");
            assertThat(result.owner()).isNull();

            verify(projectRepository).findById(projectId);
        }

        @Test
        void shouldThrownException_whenProjectNotFound() {
            Long projectId = 1L;

            when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.getProjectById(projectId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Project with id 1 not found");

            verify(projectRepository).findById(projectId);
        }
    }

    @Nested
    class CreateProject {

        @Test
        void shouldCreateProject() {
            Long userId = 1L;
            ProjectCreateRequest request = new ProjectCreateRequest("Secure Task Manager", "Backend project");
            Project savedProject = Project.builder()
                    .id(10L)
                    .name("Secure Task Manager")
                    .description("Backend project")
                    .ownerId(userId)
                    .build();

            when(userService.getCurrentUserId()).thenReturn(userId);
            when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

            ProjectResponse result = projectService.createProject(request);
            ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);

            verify(projectRepository).save(projectCaptor.capture());

            Project captured = projectCaptor.getValue();

            assertThat(captured.getName()).isEqualTo("Secure Task Manager");
            assertThat(captured.getDescription()).isEqualTo("Backend project");
            assertThat(captured.getOwnerId()).isEqualTo(userId);
            assertThat(captured.getCreatedAt()).isNotNull();
            assertThat(captured.getUpdatedAt()).isNotNull();

            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.name()).isEqualTo("Secure Task Manager");
        }

        @Test
        void shouldCreateProject_whenDescriptionIsNull() {
            Long userId = 1L;
            ProjectCreateRequest request = new ProjectCreateRequest("Secure Task Manager", null);

            Project savedProject = Project.builder()
                    .id(10L)
                    .name("Secure Task Manager")
                    .description(null)
                    .ownerId(userId)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(userService.getCurrentUserId()).thenReturn(userId);
            when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

            ProjectResponse result = projectService.createProject(request);

            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.name()).isEqualTo("Secure Task Manager");
            assertThat(result.description()).isNull();

            verify(projectRepository).save(any(Project.class));
        }

        @Test
        void shouldThrow_whenProjectWithSameNameAlreadyExists() {
            Long userId = 1L;

            ProjectCreateRequest request = new ProjectCreateRequest(
                    "Secure Task Manager",
                    "Backend project"
            );

            when(userService.getCurrentUserId()).thenReturn(userId);
            when(projectRepository.existsByOwnerIdAndName(userId, request.name()))
                    .thenReturn(true);

            assertThatThrownBy(() -> projectService.createProject(request))
                    .isInstanceOf(ProjectAlreadyExistsException.class)
                    .hasMessage("Project with name 'Secure Task Manager' already exists");

            verify(projectRepository, never()).save(any(Project.class));
        }
    }

    @Nested
    class UpdateProject {
        @Test
        void shouldUpdateProject() {
            Long projectId = 1L;

            Project project = Project.builder()
                    .id(projectId)
                    .name("Old name")
                    .description("Old description")
                    .ownerId(1L)
                    .build();

            ProjectUpdateRequest request = new ProjectUpdateRequest("New name", "New description");

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
            when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ProjectResponse result = projectService.updateProject(projectId, request);

            assertThat(result.name()).isEqualTo("New name");
            assertThat(result.description()).isEqualTo("New description");

            verify(projectRepository).findById(projectId);
            verify(projectRepository).save(any(Project.class));
        }

        @Test
        void shouldThrow_whenProjectNotFound() {
            Long projectId = 1L;
            ProjectUpdateRequest request = new ProjectUpdateRequest("New name", "New description");

            when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.updateProject(projectId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Project with id 1 not found");

            verify(projectRepository).findById(projectId);
        }

        @Test
        void shouldThrow_whenRequestIsEmpty() {
            Long projectId = 1L;
            ProjectUpdateRequest request = new ProjectUpdateRequest(null, null);
            assertThatThrownBy(() -> projectService.updateProject(projectId, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("At least one field must be provided");

            verify(projectRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteProject {

        @Test
        void shouldDeleteProject() {

            Long userId = 1L;
            Long projectId = 1L;

            Project project = Project.builder()
                    .id(projectId)
                    .name("Unnecessary project")
                    .ownerId(userId)
                    .build();

            when(userService.getCurrentUserId()).thenReturn(userId);
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

            projectService.deleteProject(projectId);

            verify(projectRepository).delete(project);
        }

        @Test
        void shouldThrow_whenProjectNotFound() {

            Long projectId = 10L;

            when(userService.getCurrentUserId()).thenReturn(1L);
            when(projectRepository.findById(projectId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.deleteProject(projectId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(projectRepository, never()).delete(any());
        }

        @Test
        void shouldThrowForbidden_whenUserIsNotOwner() {

            Long userId = 1L;
            Long anotherUserId = 2L;
            Long projectId = 10L;

            Project project = Project.builder()
                    .id(projectId)
                    .name("Unnecessary project")
                    .ownerId(anotherUserId)
                    .build();

            when(userService.getCurrentUserId()).thenReturn(userId);
            when(projectRepository.findById(projectId))
                    .thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.deleteProject(projectId))
                    .isInstanceOf(AccessDeniedException.class);

            verify(projectRepository, never()).delete(any());
        }
    }

}
