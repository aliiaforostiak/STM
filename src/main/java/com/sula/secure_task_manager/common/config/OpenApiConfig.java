package com.sula.secure_task_manager.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "secure-task-manager API",
                version = "v1",
                description = "API for secure task manager"
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {

    @Bean
    public GlobalOperationCustomizer global500ResponseCustomizer() {
        return (operation, handlerMethod) -> {
            if (operation.getResponses() == null) {
                operation.setResponses(new ApiResponses());
            }

            operation.getResponses().putIfAbsent(
                    "500",
                    new ApiResponse().description("Internal Server Error")
            );

            return operation;
        };
    }
}
