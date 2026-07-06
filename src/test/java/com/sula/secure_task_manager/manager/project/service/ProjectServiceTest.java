package com.sula.secure_task_manager.manager.project.service;

import com.sula.secure_task_manager.common.dto.PageResponse;
import com.sula.secure_task_manager.common.exception.base.BadRequestException;
import com.sula.secure_task_manager.common.exception.base.ResourceNotFoundException;
import com.sula.secure_task_manager.manager.project.dto.ProjectCreateRequest;
import com.sula.secure_task_manager.manager.project.dto.ProjectResponse;
import com.sula.secure_task_manager.manager.project.dto.ProjectShortResponse;
import com.sula.secure_task_manager.manager.project.dto.ProjectUpdateRequest;
import com.sula.secure_task_manager.manager.project.entity.Project;
import com.sula.secure_task_manager.manager.project.exception.ProjectAlreadyExistsException;
import com.sula.secure_task_manager.manager.project.repository.ProjectRepository;
import com.sula.secure_task_manager.security.principal.CurrentUserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ProjectAccessService projectAccessService;

    @InjectMocks
    private ProjectService projectService;

    @Nested
    class GetMyProjects {

        @Test
        void shouldReturnUserProjects() {
            Long userId = 1L;
            List<Project> projects = List.of(
                    Project.builder().id(1L).name("Secure Task Manager").ownerId(userId).build(),
                    Project.builder().id(2L).name("Notification Service").ownerId(userId).build()
            );

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectRepository.findAllByOwnerId(userId)).thenReturn(projects);

            List<ProjectShortResponse> result = projectService.getMyProjects();

            assertThat(result)
                    .extracting(ProjectShortResponse::id, ProjectShortResponse::name)
                    .containsExactly(
                            tuple(1L, "Secure Task Manager"),
                            tuple(2L, "Notification Service")
                    );

            verify(currentUserService).getCurrentUserId();
            verify(projectRepository).findAllByOwnerId(userId);
            verifyNoMoreInteractions(projectRepository);
        }

        @Test
        void shouldReturnEmptyList_whenUserHasNoProjects() {
            Long userId = 1L;

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectRepository.findAllByOwnerId(userId)).thenReturn(List.of());

            List<ProjectShortResponse> result = projectService.getMyProjects();

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnProjectsPage() {
            Long userId = 1L;
            List<Project> projects = List.of(
                    Project.builder().id(1L).name("Secure Task Manager").ownerId(userId).build(),
                    Project.builder().id(2L).name("Notification Service").ownerId(userId).build()
            );

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectRepository.findAllByOwnerId(userId, PageRequest.of(0, 2)))
                    .thenReturn(new PageImpl<>(projects, PageRequest.of(0, 2), 5));

            PageResponse<ProjectShortResponse> result = projectService.getMyProjectsPage(0, 2);

            assertThat(result.content()).hasSize(2);
            assertThat(result.page()).isEqualTo(0);
            assertThat(result.size()).isEqualTo(2);
            assertThat(result.totalElements()).isEqualTo(5);
            assertThat(result.totalPages()).isEqualTo(3);
        }
    }

    @Nested
    class GetMyProjectById {

        @Test
        void shouldReturnDetailedProjectById_whenUserIsOwner() {
            Long ownerId = 1L;
            Long projectId = 1L;
            Project project = Project.builder()
                    .id(projectId)
                    .name("Secure task Management")
                    .description("Backend project")
                    .ownerId(ownerId)
                    .build();

            when(currentUserService.getCurrentUserId()).thenReturn(ownerId);
            when(projectAccessService.getAccessibleProject(projectId, ownerId, "project")).thenReturn(project);

            ProjectResponse result = projectService.getProjectById(projectId);

            assertThat(result.id()).isEqualTo(projectId);
            assertThat(result.name()).isEqualTo("Secure task Management");
            assertThat(result.description()).isEqualTo("Backend project");
            assertThat(result.owner()).isNull();
        }

        @Test
        void shouldThrownException_whenProjectNotFound() {
            Long projectId = 1L;

            when(currentUserService.getCurrentUserId()).thenReturn(1L);
            when(projectAccessService.getAccessibleProject(projectId, 1L, "project"))
                    .thenThrow(new ResourceNotFoundException("Project", projectId));

            assertThatThrownBy(() -> projectService.getProjectById(projectId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Project with id 1 not found");
        }

        @Test
        void shouldThrowException_whenUserIsNotOwner() {
            Long userId = 1L;
            Long projectId = 1L;

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectAccessService.getAccessibleProject(projectId, userId, "project"))
                    .thenThrow(new AccessDeniedException("You do not have access to this project"));

            assertThatThrownBy(() -> projectService.getProjectById(projectId))
                    .isInstanceOf(AccessDeniedException.class);

            verify(projectRepository, never()).save(any());
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

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectRepository.existsByOwnerIdAndName(userId, request.name())).thenReturn(false);
            when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

            ProjectResponse result = projectService.createProject(request);
            ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);

            verify(projectRepository).save(projectCaptor.capture());
            Project captured = projectCaptor.getValue();

            assertThat(captured.getName()).isEqualTo("Secure Task Manager");
            assertThat(captured.getDescription()).isEqualTo("Backend project");
            assertThat(captured.getOwnerId()).isEqualTo(userId);
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
                    .build();

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectRepository.existsByOwnerIdAndName(userId, request.name())).thenReturn(false);
            when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

            ProjectResponse result = projectService.createProject(request);

            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.name()).isEqualTo("Secure Task Manager");
            assertThat(result.description()).isNull();
        }

        @Test
        void shouldTrimProjectName_whenCreatingProject() {
            Long userId = 1L;
            ProjectCreateRequest request = new ProjectCreateRequest("  Secure Task Manager  ", "Backend project");
            Project savedProject = Project.builder()
                    .id(10L)
                    .name("Secure Task Manager")
                    .description("Backend project")
                    .ownerId(userId)
                    .build();

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectRepository.existsByOwnerIdAndName(userId, "Secure Task Manager")).thenReturn(false);
            when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

            ProjectResponse result = projectService.createProject(request);
            ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);

            verify(projectRepository).save(projectCaptor.capture());

            assertThat(projectCaptor.getValue().getName()).isEqualTo("Secure Task Manager");
            assertThat(result.name()).isEqualTo("Secure Task Manager");
        }

        @Test
        void shouldThrow_whenProjectWithSameNameAlreadyExists() {
            Long userId = 1L;
            ProjectCreateRequest request = new ProjectCreateRequest("Secure Task Manager", "Backend project");

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectRepository.existsByOwnerIdAndName(userId, request.name())).thenReturn(true);

            assertThatThrownBy(() -> projectService.createProject(request))
                    .isInstanceOf(ProjectAlreadyExistsException.class)
                    .hasMessage("Project with name 'Secure Task Manager' already exists");

            verify(projectRepository, never()).save(any(Project.class));
        }

        @Test
        void shouldThrow_whenUniqueConstraintIsViolatedDuringSave() {
            Long userId = 1L;
            ProjectCreateRequest request = new ProjectCreateRequest("Secure Task Manager", "Backend project");

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectRepository.existsByOwnerIdAndName(userId, "Secure Task Manager")).thenReturn(false);
            when(projectRepository.save(any(Project.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate", new RuntimeException("uq_projects_owner_name")));

            assertThatThrownBy(() -> projectService.createProject(request))
                    .isInstanceOf(ProjectAlreadyExistsException.class)
                    .hasMessage("Project with name 'Secure Task Manager' already exists");
        }
    }

    @Nested
    class UpdateProject {

        @Test
        void shouldUpdateProject_whenUserIsOwner() {
            Long userId = 1L;
            Long projectId = 1L;
            Project project = Project.builder()
                    .id(projectId)
                    .name("Old name")
                    .description("Old description")
                    .ownerId(userId)
                    .build();
            ProjectUpdateRequest request = new ProjectUpdateRequest("New name", "New description");

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectAccessService.getAccessibleProject(projectId, userId, "project")).thenReturn(project);
            when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ProjectResponse result = projectService.updateProject(projectId, request);

            assertThat(project.getName()).isEqualTo("New name");
            assertThat(project.getDescription()).isEqualTo("New description");
            assertThat(result.name()).isEqualTo("New name");
        }

        @Test
        void shouldThrow_whenProjectNotFound() {
            Long projectId = 1L;
            ProjectUpdateRequest request = new ProjectUpdateRequest("New name", "New description");

            when(currentUserService.getCurrentUserId()).thenReturn(1L);
            when(projectAccessService.getAccessibleProject(projectId, 1L, "project"))
                    .thenThrow(new ResourceNotFoundException("Project", projectId));

            assertThatThrownBy(() -> projectService.updateProject(projectId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Project with id 1 not found");
        }

        @Test
        void shouldThrow_whenRequestIsEmpty() {
            ProjectUpdateRequest request = new ProjectUpdateRequest(null, null);

            assertThatThrownBy(() -> projectService.updateProject(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("At least one field must be provided");

            verify(projectRepository, never()).save(any());
        }

        @Test
        void shouldThrowForbidden_whenUserIsNotOwner() {
            Long userId = 1L;
            Long projectId = 1L;
            ProjectUpdateRequest request = new ProjectUpdateRequest("New name", "New description");

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectAccessService.getAccessibleProject(projectId, userId, "project"))
                    .thenThrow(new AccessDeniedException("You do not have access to this project"));

            assertThatThrownBy(() -> projectService.updateProject(projectId, request))
                    .isInstanceOf(AccessDeniedException.class);

            verify(projectRepository, never()).save(any());
        }

        @Test
        void shouldUpdateOnlyName_whenDescriptionIsNull() {
            Long userId = 1L;
            Long projectId = 1L;
            Project project = Project.builder()
                    .id(projectId)
                    .name("Old name")
                    .description("Old description")
                    .ownerId(userId)
                    .build();
            ProjectUpdateRequest request = new ProjectUpdateRequest("New name", null);

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectAccessService.getAccessibleProject(projectId, userId, "project")).thenReturn(project);
            when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ProjectResponse result = projectService.updateProject(projectId, request);

            assertThat(result.name()).isEqualTo("New name");
            assertThat(result.description()).isEqualTo("Old description");
        }

        @Test
        void shouldUpdateOnlyDescription_whenNameIsNull() {
            Long userId = 1L;
            Long projectId = 1L;
            Project project = Project.builder()
                    .id(projectId)
                    .name("Old name")
                    .description("Old description")
                    .ownerId(userId)
                    .build();
            ProjectUpdateRequest request = new ProjectUpdateRequest(null, "New description");

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectAccessService.getAccessibleProject(projectId, userId, "project")).thenReturn(project);
            when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ProjectResponse result = projectService.updateProject(projectId, request);

            assertThat(result.name()).isEqualTo("Old name");
            assertThat(result.description()).isEqualTo("New description");
        }

        @Test
        void shouldTrimProjectName_whenUpdatingProject() {
            Long userId = 1L;
            Long projectId = 1L;
            Project project = Project.builder()
                    .id(projectId)
                    .name("Old name")
                    .description("Old description")
                    .ownerId(userId)
                    .build();
            ProjectUpdateRequest request = new ProjectUpdateRequest("  New name  ", null);

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectAccessService.getAccessibleProject(projectId, userId, "project")).thenReturn(project);
            when(projectRepository.existsByOwnerIdAndName(userId, "New name")).thenReturn(false);
            when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ProjectResponse result = projectService.updateProject(projectId, request);

            assertThat(project.getName()).isEqualTo("New name");
            assertThat(result.name()).isEqualTo("New name");
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

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectAccessService.getAccessibleProject(projectId, userId, "project")).thenReturn(project);

            projectService.deleteProject(projectId);

            verify(projectRepository).delete(project);
            verifyNoMoreInteractions(projectRepository);
        }

        @Test
        void shouldThrow_whenProjectNotFound() {
            Long projectId = 10L;

            when(currentUserService.getCurrentUserId()).thenReturn(1L);
            when(projectAccessService.getAccessibleProject(projectId, 1L, "project"))
                    .thenThrow(new ResourceNotFoundException("Project", projectId));

            assertThatThrownBy(() -> projectService.deleteProject(projectId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(projectRepository, never()).delete(any());
        }

        @Test
        void shouldThrowForbidden_whenUserIsNotOwner() {
            Long userId = 1L;
            Long projectId = 10L;

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectAccessService.getAccessibleProject(projectId, userId, "project"))
                    .thenThrow(new AccessDeniedException("You do not have access to this project"));

            assertThatThrownBy(() -> projectService.deleteProject(projectId))
                    .isInstanceOf(AccessDeniedException.class);

            verify(projectRepository, never()).delete(any());
        }
    }
}
