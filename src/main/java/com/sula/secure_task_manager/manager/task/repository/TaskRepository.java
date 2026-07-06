package com.sula.secure_task_manager.manager.task.repository;

import com.sula.secure_task_manager.manager.task.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByProject_Id(Long projectId);

    Page<Task> findAllByProject_Id(Long projectId, Pageable pageable);
}
