package com.sula.secure_task_manager.manager.user.exception;

import com.sula.secure_task_manager.common.exception.base.ApiException;
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
