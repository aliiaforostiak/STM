package com.sula.secure_task_manager.manager.project.repository;

import com.sula.secure_task_manager.manager.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByOwnerId(Long ownerId);

    boolean existsByOwnerIdAndName(Long ownerId, String name);
}
