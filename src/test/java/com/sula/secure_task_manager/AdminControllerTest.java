package com.sula.secure_task_manager;

import com.sula.secure_task_manager.admin.controller.AdminController;
import com.sula.secure_task_manager.admin.dto.AdminPingResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AdminControllerTest {

    @Test
    void ping_returnsOk() {
        AdminPingResponse response = new AdminController().ping();

        assertThat(response.status()).isEqualTo("ok");
    }

    @Test
    void ping_isProtected() throws Exception {
        Method method = AdminController.class.getDeclaredMethod("ping");

        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
        assertThat(AdminController.class.isAnnotationPresent(SecurityRequirement.class)).isTrue();
    }
}
