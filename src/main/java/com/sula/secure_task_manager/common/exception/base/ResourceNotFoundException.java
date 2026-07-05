package com.sula.secure_task_manager.common.exception.base;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resource, Object id) {
        super(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                resource + " with id " + id + " not found",
                resource.toLowerCase()
        );
    }
}
