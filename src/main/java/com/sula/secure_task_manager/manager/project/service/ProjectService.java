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
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final String PROJECT_OWNER_NAME_CONSTRAINT = "uq_projects_owner_name";

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
        String normalizedName = normalizeProjectName(request.name());

        if (projectRepository.existsByOwnerIdAndName(ownerId, normalizedName)) {
            throw new ProjectAlreadyExistsException(normalizedName);
        }

        Project project = Project.builder()
                .name(normalizedName)
                .description(request.description())
                .ownerId(ownerId)
                .build();

        Project savedProject = saveProject(project, normalizedName);

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
            String normalizedName = normalizeProjectName(request.name());

            if (!normalizedName.equals(projectToUpdate.getName())
                    && projectRepository.existsByOwnerIdAndName(userId, normalizedName)) {
                throw new ProjectAlreadyExistsException(normalizedName);
            }

            projectToUpdate.setName(normalizedName);
        }

        if (request.description() != null) {
            projectToUpdate.setDescription(request.description());
        }

        Project saved = saveProject(projectToUpdate, projectToUpdate.getName());

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

    private Project saveProject(Project project, String projectName) {
        try {
            return projectRepository.save(project);
        } catch (DataIntegrityViolationException exception) {
            if (isProjectNameConflict(exception)) {
                throw new ProjectAlreadyExistsException(projectName);
            }

            throw exception;
        }
    }

    private String normalizeProjectName(String name) {
        String normalizedName = name.strip();

        if (normalizedName.isBlank()) {
            throw new BadRequestException("Project name is required", "name");
        }

        return normalizedName;
    }

    private boolean isProjectNameConflict(DataIntegrityViolationException exception) {
        Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(exception);
        return rootCause != null
                && rootCause.getMessage() != null
                && rootCause.getMessage().contains(PROJECT_OWNER_NAME_CONSTRAINT);
    }
}
