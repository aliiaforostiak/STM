package com.sula.secure_task_manager.common.exception;

import org.springframework.http.HttpStatus;

public class ProjectAlreadyExistsException extends ApiException {

    public ProjectAlreadyExistsException(String name) {
        super(
                HttpStatus.CONFLICT,
                "Project_ALREADY_EXISTS",
                "Project with name '" + name + "' already exists",
                "name"
        );
    }
}
