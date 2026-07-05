package com.sula.secure_task_manager.manager.task.service;

import com.sula.secure_task_manager.common.exception.base.BadRequestException;
import com.sula.secure_task_manager.common.exception.base.ResourceNotFoundException;
import com.sula.secure_task_manager.manager.project.dto.ProjectShortResponse;
import com.sula.secure_task_manager.manager.project.entity.Project;
import com.sula.secure_task_manager.manager.project.repository.ProjectRepository;
import com.sula.secure_task_manager.manager.task.dto.TaskCreateRequest;
import com.sula.secure_task_manager.manager.task.dto.TaskResponse;
import com.sula.secure_task_manager.manager.task.dto.TaskShortResponse;
import com.sula.secure_task_manager.manager.task.dto.TaskStatus;
import com.sula.secure_task_manager.manager.task.dto.TaskUpdateRequest;
import com.sula.secure_task_manager.manager.task.entity.Task;
import com.sula.secure_task_manager.manager.task.repository.TaskRepository;
import com.sula.secure_task_manager.manager.user.User;
import com.sula.secure_task_manager.manager.user.UserRepository;
import com.sula.secure_task_manager.manager.user.dto.UserShortResponse;
import com.sula.secure_task_manager.security.principal.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public TaskResponse getTaskById(Long id) {
        Task task = getTask(id);
        Project project = getAccessibleProject(task.getProject(), "task");

        User creator = getUser(task.getCreatorId());
        User assignee = task.getAssigneeId() != null ? getUser(task.getAssigneeId()) : null;

        return toResponse(task, project, creator, assignee);
    }

    public List<TaskShortResponse> getProjectTasks(Long projectId) {
        getAccessibleProject(projectId, "project");

        return taskRepository.findAllByProject_Id(projectId).stream()
                .map(task -> new TaskShortResponse(
                        task.getId(),
                        task.getTitle(),
                        task.getStatus(),
                        task.getPriority()
                ))
                .toList();
    }

    public TaskResponse createTask(TaskCreateRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Project project = getAccessibleProject(request.projectId(), "project");

        User assignee = request.assigneeId() != null ? getUser(request.assigneeId()) : null;
        User creator = getUser(currentUserId);

        Task task = Task.builder()
                .title(request.title().strip())
                .description(request.description())
                .priority(request.priority())
                .status(TaskStatus.TODO)
                .project(project)
                .creatorId(currentUserId)
                .assigneeId(request.assigneeId())
                .dueDate(request.dueDate())
                .build();

        Task savedTask = taskRepository.save(task);
        return toResponse(savedTask, project, creator, assignee);
    }

    public TaskResponse updateTask(Long id, TaskUpdateRequest request) {
        if (isEmpty(request)) {
            throw new BadRequestException("At least one field must be provided");
        }

        Task task = getTask(id);
        Project project = getAccessibleProject(task.getProject(), "task");

        if (request.title() != null) {
            String normalizedTitle = request.title().strip();
            if (normalizedTitle.isBlank()) {
                throw new BadRequestException("Task title is required", "title");
            }
            task.setTitle(normalizedTitle);
        }

        if (request.description() != null) {
            task.setDescription(request.description());
        }

        if (request.priority() != null) {
            task.setPriority(request.priority());
        }

        if (request.assigneeId() != null) {
            getUser(request.assigneeId());
            task.setAssigneeId(request.assigneeId());
        }

        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }

        if (request.status() != null && request.status() != task.getStatus()) {
            if (!task.getStatus().canMoveTo(request.status())) {
                throw new BadRequestException("Invalid task status transition", "status");
            }

            task.setStatus(request.status());

            if (request.status() == TaskStatus.DONE) {
                task.setCompletedAt(java.time.Instant.now());
            }
        }

        Task savedTask = taskRepository.save(task);
        User creator = getUser(savedTask.getCreatorId());
        User assignee = savedTask.getAssigneeId() != null ? getUser(savedTask.getAssigneeId()) : null;

        return toResponse(savedTask, project, creator, assignee);
    }

    public void deleteTask(Long id) {
        Task task = getTask(id);
        getAccessibleProject(task.getProject(), "task");
        taskRepository.delete(task);
    }

    private Task getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    private Project getAccessibleProject(Long projectId, String resourceName) {
        Long currentUserId = currentUserService.getCurrentUserId();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        if (!project.getOwnerId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have access to this " + resourceName);
        }

        return project;
    }

    private Project getAccessibleProject(Project project, String resourceName) {
        Long currentUserId = currentUserService.getCurrentUserId();

        if (!project.getOwnerId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have access to this " + resourceName);
        }

        return project;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private TaskResponse toResponse(Task task, Project project, User creator, User assignee) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getStatus(),
                new ProjectShortResponse(project.getId(), project.getName()),
                new UserShortResponse(creator.getId(), creator.getEmail()),
                assignee != null ? new UserShortResponse(assignee.getId(), assignee.getEmail()) : null,
                task.getDueDate(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private boolean isEmpty(TaskUpdateRequest request) {
        return request.title() == null
                && request.description() == null
                && request.priority() == null
                && request.status() == null
                && request.assigneeId() == null
                && request.dueDate() == null;
    }
}
