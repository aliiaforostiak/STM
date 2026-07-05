package com.sula.secure_task_manager.manager.task.service;

import com.sula.secure_task_manager.manager.task.dto.TaskCreateRequest;
import com.sula.secure_task_manager.manager.task.dto.TaskResponse;
import com.sula.secure_task_manager.manager.task.dto.TaskShortResponse;
import com.sula.secure_task_manager.manager.task.dto.TaskUpdateRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    public TaskResponse getTaskById(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<TaskShortResponse> getProjectTasks(Long projectId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public TaskResponse createTask(TaskCreateRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public TaskResponse updateTask(Long id, TaskUpdateRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void deleteTask(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
