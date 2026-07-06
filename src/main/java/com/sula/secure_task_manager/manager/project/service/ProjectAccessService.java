package com.sula.secure_task_manager.manager.project.service;

import com.sula.secure_task_manager.common.exception.base.ResourceNotFoundException;
import com.sula.secure_task_manager.manager.project.entity.Project;
import com.sula.secure_task_manager.manager.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final ProjectRepository projectRepository;

    public Project getAccessibleProject(Long projectId, Long userId, String resourceName) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        assertOwner(project, userId, resourceName);
        return project;
    }

    public Project getAccessibleProject(Project project, Long userId, String resourceName) {
        assertOwner(project, userId, resourceName);
        return project;
    }

    public void assertOwner(Project project, Long userId, String resourceName) {
        if (!project.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("You do not have access to this " + resourceName);
        }
    }
}
