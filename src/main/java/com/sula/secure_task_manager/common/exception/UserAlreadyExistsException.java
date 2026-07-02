package com.sula.secure_task_manager.common.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends ApiException {

    public UserAlreadyExistsException(String message) {
        super(
                HttpStatus.CONFLICT,
                "USER_ALREADY_EXISTS",
                message,
                "email"
        );
    }
}
