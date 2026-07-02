package com.sula.secure_task_manager.manager.service;

import com.sula.secure_task_manager.manager.dto.project.ProjectCreateRequest;
import com.sula.secure_task_manager.manager.dto.project.ProjectResponse;
import com.sula.secure_task_manager.manager.dto.project.ProjectShortResponse;
import com.sula.secure_task_manager.manager.dto.project.ProjectUpdateRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {
    public List<ProjectShortResponse> getMyProjects() {
        return null;
    }

    public ProjectResponse getProjectById(Long id) {
        return null;
    }

    public ProjectResponse createProject(ProjectCreateRequest request) {
        return null;
    }

    public ProjectResponse updateProject(Long id, ProjectUpdateRequest request) {
        return null;
    }

    public void deleteProject(long id) {
    }
}
