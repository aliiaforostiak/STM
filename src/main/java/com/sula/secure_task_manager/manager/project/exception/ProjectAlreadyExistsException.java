package com.sula.secure_task_manager.manager.project.exception;

import com.sula.secure_task_manager.common.exception.base.ApiException;
import org.springframework.http.HttpStatus;

public class ProjectAlreadyExistsException extends ApiException {

    public ProjectAlreadyExistsException(String name) {
        super(
                HttpStatus.CONFLICT,
                "PROJECT_ALREADY_EXISTS",
                "Project with name '" + name + "' already exists",
                "name"
        );
    }
}
