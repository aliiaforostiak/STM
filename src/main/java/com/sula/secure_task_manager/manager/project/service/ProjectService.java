package com.sula.secure_task_manager.manager.project.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;

    public List<ProjectShortResponse> getMyProjects() {
        Long ownerId = currentUserService.getCurrentUserId();
        List<Project> projects = projectRepository.findAllByOwnerId(ownerId);

        return projects.stream()
                .map(this::toShortResponse)
                .toList();
    }

    private ProjectShortResponse toShortResponse(Project project) {
        return new ProjectShortResponse(project.getId(), project.getName());
    }

    public ProjectResponse getProjectById(Long projectId) {

        Long userId = currentUserService.getCurrentUserId();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        if (!project.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("Yau are not owner of this project");
        }

        return toResponse(project);
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                null,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    public ProjectResponse createProject(ProjectCreateRequest request) {

        Long ownerId = currentUserService.getCurrentUserId();

        if (projectRepository.existsByOwnerIdAndName(ownerId, request.name())) {
            throw new ProjectAlreadyExistsException(request.name());
        }

        Instant now = Instant.now();

        Project project = Project.builder()
                .name(request.name())
                .description(request.description())
                .ownerId(ownerId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Project savedProject = projectRepository.save(project);

        return toResponse(savedProject);
    }

    public ProjectResponse updateProject(Long projectId, ProjectUpdateRequest request) {

        if (request.name() == null && request.description() == null) {
            throw new BadRequestException("At least one field must be provided");
        }

        Long userId = currentUserService.getCurrentUserId();

        Project projectToUpdate = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        if (!projectToUpdate.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("You are not owner of this project");
        }

        if (request.name() != null) {
            projectToUpdate.setName(request.name());
        }

        if (request.description() != null) {
            projectToUpdate.setDescription(request.description());
        }

        projectToUpdate.setUpdatedAt(Instant.now());

        Project saved = projectRepository.save(projectToUpdate);

        return toResponse(saved);
    }

    public void deleteProject(Long projectId) {

        Long userId = currentUserService.getCurrentUserId();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        if (!project.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("You are not owner of this project");
        }

        projectRepository.delete(project);
    }
}
