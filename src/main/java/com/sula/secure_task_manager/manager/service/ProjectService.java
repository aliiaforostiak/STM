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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserService userService;

    public List<ProjectShortResponse> getMyProjects() {
        Long ownerId = userService.getCurrentUserId();
        List<Project> projects = projectRepository.findAllByOwnerId(ownerId);

        return projects.stream()
                .map(this::toShortResponse)
                .toList();
    }

    private ProjectShortResponse toShortResponse(Project project) {
        return new ProjectShortResponse(project.getId(), project.getName());
    }

    public ProjectResponse getProjectById(Long projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
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

        Long ownerId = userService.getCurrentUserId();

        if (projectRepository.existsByOwnerIdAndName(ownerId, request.name())) {
            throw new ProjectAlreadyExistsException(request.name());
        }

        Project project = Project.builder()
                .name(request.name())
                .description(request.description())
                .ownerId(ownerId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Project savedProject = projectRepository.save(project);

        return toResponse(savedProject);
    }

    public ProjectResponse updateProject(Long projectId, ProjectUpdateRequest request) {

        if(request.name() == null && request.description() == null){
            throw new BadRequestException("At least one field must be provided");
        }

        Project projectToUpdate = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

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

        Long userId = userService.getCurrentUserId();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        if(!project.getOwnerId().equals(userId)){
            throw new AccessDeniedException("You are not ower of this project");
        }

        projectRepository.delete(project);
    }
}
