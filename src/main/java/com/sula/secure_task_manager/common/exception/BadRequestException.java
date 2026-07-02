package com.sula.secure_task_manager.common.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        this(message, "request");
    }

    public BadRequestException(String message, String field) {
        super(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                message,
                field
        );
    }
}
