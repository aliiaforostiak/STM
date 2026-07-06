package com.sula.secure_task_manager.manager.task.service;

import com.sula.secure_task_manager.common.dto.PageResponse;
import com.sula.secure_task_manager.common.exception.base.BadRequestException;
import com.sula.secure_task_manager.common.exception.base.ResourceNotFoundException;
import com.sula.secure_task_manager.manager.project.dto.ProjectShortResponse;
import com.sula.secure_task_manager.manager.project.entity.Project;
import com.sula.secure_task_manager.manager.project.service.ProjectAccessService;
import com.sula.secure_task_manager.manager.task.dto.TaskCreateRequest;
import com.sula.secure_task_manager.manager.task.dto.TaskPriority;
import com.sula.secure_task_manager.manager.task.dto.TaskResponse;
import com.sula.secure_task_manager.manager.task.dto.TaskShortResponse;
import com.sula.secure_task_manager.manager.task.dto.TaskStatus;
import com.sula.secure_task_manager.manager.task.dto.TaskUpdateRequest;
import com.sula.secure_task_manager.manager.task.entity.Task;
import com.sula.secure_task_manager.manager.task.repository.TaskRepository;
import com.sula.secure_task_manager.manager.user.Role;
import com.sula.secure_task_manager.manager.user.User;
import com.sula.secure_task_manager.manager.user.UserRepository;
import com.sula.secure_task_manager.manager.user.dto.UserShortResponse;
import com.sula.secure_task_manager.security.principal.CurrentUserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private TaskService taskService;

    @Nested
    class GetTaskById {

        @Test
        void shouldReturnTask_whenUserOwnsProject() {
            Long userId = 1L;
            Long taskId = 10L;
            Long projectId = 20L;

            Project project = project(projectId, userId, "Secure Task Manager");
            User creator = user(7L, "creator@example.com");
            User assignee = user(8L, "assignee@example.com");
            Task task = task(taskId, project, creator.getId(), assignee.getId(), TaskStatus.TODO);

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
            when(projectAccessService.getAccessibleProject(project, userId, "task")).thenReturn(project);
            when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
            when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));

            TaskResponse result = taskService.getTaskById(taskId);

            assertThat(result.id()).isEqualTo(taskId);
            assertThat(result.title()).isEqualTo("Implement JWT login");
            assertThat(result.project()).isEqualTo(new ProjectShortResponse(projectId, "Secure Task Manager"));
            assertThat(result.creator()).isEqualTo(new UserShortResponse(7L, "creator@example.com"));
            assertThat(result.assignee()).isEqualTo(new UserShortResponse(8L, "assignee@example.com"));
        }

        @Test
        void shouldThrowNotFound_whenTaskDoesNotExist() {
            when(taskRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTaskById(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Task with id 10 not found");
        }

        @Test
        void shouldThrowForbidden_whenUserDoesNotOwnTaskProject() {
            Long userId = 1L;
            Project project = project(20L, 99L, "Secure Task Manager");
            Task task = task(10L, project, 7L, 8L, TaskStatus.TODO);

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
            when(projectAccessService.getAccessibleProject(project, userId, "task"))
                    .thenThrow(new AccessDeniedException("You do not have access to this task"));

            assertThatThrownBy(() -> taskService.getTaskById(10L))
                    .isInstanceOf(AccessDeniedException.class);

            verify(userRepository, never()).findById(any());
        }
    }

    @Nested
    class GetProjectTasks {

        @Test
        void shouldReturnProjectTasks_whenUserOwnsProject() {
            Long userId = 1L;
            Long projectId = 20L;
            Project project = project(projectId, userId, "Secure Task Manager");

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectAccessService.getAccessibleProject(projectId, userId, "project")).thenReturn(project);
            when(taskRepository.findAllByProject_Id(projectId)).thenReturn(List.of(
                    task(10L, project, 7L, 8L, TaskStatus.TODO),
                    task(11L, project, 7L, null, TaskStatus.IN_PROGRESS)
            ));

            List<TaskShortResponse> result = taskService.getProjectTasks(projectId);

            assertThat(result)
                    .extracting(TaskShortResponse::id, TaskShortResponse::title, TaskShortResponse::status, TaskShortResponse::priority)
                    .containsExactly(
                            tuple(10L, "Implement JWT login", TaskStatus.TODO, TaskPriority.HIGH),
                            tuple(11L, "Implement JWT login", TaskStatus.IN_PROGRESS, TaskPriority.HIGH)
                    );
        }

        @Test
        void shouldThrowForbidden_whenUserDoesNotOwnProject() {
            when(currentUserService.getCurrentUserId()).thenReturn(1L);
            when(projectAccessService.getAccessibleProject(20L, 1L, "project"))
                    .thenThrow(new AccessDeniedException("You do not have access to this project"));

            assertThatThrownBy(() -> taskService.getProjectTasks(20L))
                    .isInstanceOf(AccessDeniedException.class);

            verify(taskRepository, never()).findAllByProject_Id(any());
        }

        @Test
        void shouldReturnProjectTasksPage_whenUserOwnsProject() {
            Long userId = 1L;
            Long projectId = 20L;
            Project project = project(projectId, userId, "Secure Task Manager");
            List<Task> tasks = List.of(
                    task(10L, project, 7L, 8L, TaskStatus.TODO),
                    task(11L, project, 7L, null, TaskStatus.IN_PROGRESS)
            );

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectAccessService.getAccessibleProject(projectId, userId, "project")).thenReturn(project);
            when(taskRepository.findAllByProject_Id(projectId, PageRequest.of(0, 2)))
                    .thenReturn(new PageImpl<>(tasks, PageRequest.of(0, 2), 3));

            PageResponse<TaskShortResponse> result = taskService.getProjectTasksPage(projectId, 0, 2);

            assertThat(result.content()).hasSize(2);
            assertThat(result.page()).isEqualTo(0);
            assertThat(result.size()).isEqualTo(2);
            assertThat(result.totalElements()).isEqualTo(3);
            assertThat(result.totalPages()).isEqualTo(2);
        }
    }

    @Nested
    class CreateTask {

        @Test
        void shouldCreateTask() {
            Long userId = 1L;
            Long projectId = 20L;
            Project project = project(projectId, userId, "Secure Task Manager");
            User assignee = user(8L, "assignee@example.com");
            TaskCreateRequest request = new TaskCreateRequest(
                    "Implement JWT login",
                    "Add access token generation and login endpoint",
                    projectId,
                    assignee.getId(),
                    TaskPriority.HIGH,
                    Instant.parse("2026-07-10T10:00:00Z")
            );

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectAccessService.getAccessibleProject(projectId, userId, "project")).thenReturn(project);
            when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, "owner@example.com")));
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
                Task task = invocation.getArgument(0);
                task.setId(10L);
                return task;
            });

            TaskResponse result = taskService.createTask(request);
            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);

            verify(taskRepository).save(taskCaptor.capture());
            Task captured = taskCaptor.getValue();

            assertThat(captured.getTitle()).isEqualTo("Implement JWT login");
            assertThat(captured.getProject().getId()).isEqualTo(projectId);
            assertThat(captured.getCreatorId()).isEqualTo(userId);
            assertThat(captured.getAssigneeId()).isEqualTo(assignee.getId());
            assertThat(captured.getPriority()).isEqualTo(TaskPriority.HIGH);
            assertThat(captured.getStatus()).isEqualTo(TaskStatus.TODO);
            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.status()).isEqualTo(TaskStatus.TODO);
            assertThat(result.creator()).isEqualTo(new UserShortResponse(userId, "owner@example.com"));
            assertThat(result.assignee()).isEqualTo(new UserShortResponse(8L, "assignee@example.com"));
        }

        @Test
        void shouldThrowNotFound_whenAssigneeDoesNotExist() {
            Long userId = 1L;
            Long projectId = 20L;
            Project project = project(projectId, userId, "Secure Task Manager");
            TaskCreateRequest request = new TaskCreateRequest(
                    "Implement JWT login",
                    "Add access token generation and login endpoint",
                    projectId,
                    8L,
                    TaskPriority.HIGH,
                    Instant.parse("2026-07-10T10:00:00Z")
            );

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(projectAccessService.getAccessibleProject(projectId, userId, "project")).thenReturn(project);
            when(userRepository.findById(8L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.createTask(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User with id 8 not found");

            verify(taskRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateTask {

        @Test
        void shouldUpdateTaskAndMarkCompletedAt_whenStatusMovesToDone() {
            Long userId = 1L;
            Long taskId = 10L;
            Long projectId = 20L;
            User creator = user(7L, "creator@example.com");
            User assignee = user(8L, "assignee@example.com");
            Project project = project(projectId, userId, "Secure Task Manager");
            Task task = task(taskId, project, creator.getId(), assignee.getId(), TaskStatus.IN_PROGRESS);
            TaskUpdateRequest request = new TaskUpdateRequest(
                    "Implement refresh flow",
                    "Add refresh endpoint and token rotation",
                    TaskPriority.CRITICAL,
                    TaskStatus.DONE,
                    assignee.getId(),
                    Instant.parse("2026-07-11T10:00:00Z")
            );

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
            when(projectAccessService.getAccessibleProject(project, userId, "task")).thenReturn(project);
            when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
            when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskResponse result = taskService.updateTask(taskId, request);

            assertThat(task.getTitle()).isEqualTo("Implement refresh flow");
            assertThat(task.getDescription()).isEqualTo("Add refresh endpoint and token rotation");
            assertThat(task.getPriority()).isEqualTo(TaskPriority.CRITICAL);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
            assertThat(task.getCompletedAt()).isNotNull();
            assertThat(result.status()).isEqualTo(TaskStatus.DONE);
        }

        @Test
        void shouldThrowBadRequest_whenStatusTransitionIsInvalid() {
            Long userId = 1L;
            Project project = project(20L, userId, "Secure Task Manager");
            Task task = task(10L, project, 7L, 8L, TaskStatus.TODO);
            TaskUpdateRequest request = new TaskUpdateRequest(null, null, null, TaskStatus.DONE, null, null);

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
            when(projectAccessService.getAccessibleProject(project, userId, "task")).thenReturn(project);

            assertThatThrownBy(() -> taskService.updateTask(10L, request))
                    .isInstanceOf(BadRequestException.class);

            verify(taskRepository, never()).save(any());
        }

        @Test
        void shouldThrowBadRequest_whenRequestIsEmpty() {
            TaskUpdateRequest request = new TaskUpdateRequest(null, null, null, null, null, null);

            assertThatThrownBy(() -> taskService.updateTask(10L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("At least one field must be provided");
        }
    }

    @Nested
    class DeleteTask {

        @Test
        void shouldDeleteTask_whenUserOwnsProject() {
            Long userId = 1L;
            Project project = project(20L, userId, "Secure Task Manager");
            Task task = task(10L, project, 7L, 8L, TaskStatus.TODO);

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
            when(projectAccessService.getAccessibleProject(project, userId, "task")).thenReturn(project);

            taskService.deleteTask(10L);

            verify(taskRepository).delete(task);
            verifyNoMoreInteractions(taskRepository);
        }

        @Test
        void shouldThrowForbidden_whenUserDoesNotOwnProject() {
            Long userId = 1L;
            Project project = project(20L, 99L, "Secure Task Manager");
            Task task = task(10L, project, 7L, 8L, TaskStatus.TODO);

            when(currentUserService.getCurrentUserId()).thenReturn(userId);
            when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
            when(projectAccessService.getAccessibleProject(project, userId, "task"))
                    .thenThrow(new AccessDeniedException("You do not have access to this task"));

            assertThatThrownBy(() -> taskService.deleteTask(10L))
                    .isInstanceOf(AccessDeniedException.class);

            verify(taskRepository, never()).delete(any());
        }
    }

    private Task task(Long id, Project project, Long creatorId, Long assigneeId, TaskStatus status) {
        return Task.builder()
                .id(id)
                .title("Implement JWT login")
                .description("Add access token generation and login endpoint")
                .priority(TaskPriority.HIGH)
                .status(status)
                .project(project)
                .creatorId(creatorId)
                .assigneeId(assigneeId)
                .dueDate(Instant.parse("2026-07-10T10:00:00Z"))
                .build();
    }

    private Project project(Long id, Long ownerId, String name) {
        return Project.builder()
                .id(id)
                .ownerId(ownerId)
                .name(name)
                .build();
    }

    private User user(Long id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .password("encoded")
                .role(Role.USER)
                .build();
    }
}
