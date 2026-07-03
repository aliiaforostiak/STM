package com.sula.secure_task_manager.manager.repository;

import com.sula.secure_task_manager.manager.entity.Project;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByOwnerId(Long ownerId);

    boolean existsByOwnerIdAndName(Long ownerId, String name);
}
