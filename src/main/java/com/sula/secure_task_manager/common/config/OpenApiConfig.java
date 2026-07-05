package com.sula.secure_task_manager.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

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

    private static final String BEARER_AUTH = "bearerAuth";

    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout"
    );

    @Bean
    public GlobalOperationCustomizer globalOperationCustomizer() {
        return this::customizeOperation;
    }

    private Operation customizeOperation(Operation operation, HandlerMethod handlerMethod) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }

        operation.getResponses().putIfAbsent(
                "500",
                new ApiResponse().description("Internal Server Error")
        );

        String path = resolvePath(handlerMethod);
        if (path == null || isPublicAuthPath(path)) {
            return operation;
        }

        operation.getResponses().putIfAbsent(
                "401",
                new ApiResponse().description("Unauthorized")
        );

        operation.getResponses().putIfAbsent(
                "403",
                new ApiResponse().description("Forbidden")
        );

        if (operation.getSecurity() == null || operation.getSecurity().stream()
                .noneMatch(requirement -> requirement.containsKey(BEARER_AUTH))) {
            operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
        }

        return operation;
    }

    private boolean isPublicAuthPath(String path) {
        return PUBLIC_AUTH_PATHS.contains(path);
    }

    private String resolvePath(HandlerMethod handlerMethod) {
        Set<String> classPaths = extractPaths(handlerMethod.getBeanType().getAnnotation(RequestMapping.class));
        if (classPaths.isEmpty()) {
            classPaths = Set.of("");
        }

        Set<String> methodPaths = extractMethodPaths(handlerMethod);
        if (methodPaths.isEmpty()) {
            methodPaths = Set.of("");
        }

        for (String classPath : classPaths) {
            for (String methodPath : methodPaths) {
                return normalizePath(classPath + "/" + methodPath);
            }
        }

        return null;
    }

    private Set<String> extractMethodPaths(HandlerMethod handlerMethod) {
        if (handlerMethod.getMethod().isAnnotationPresent(GetMapping.class)) {
            return extractPaths(handlerMethod.getMethod().getAnnotation(GetMapping.class).value(),
                    handlerMethod.getMethod().getAnnotation(GetMapping.class).path());
        }
        if (handlerMethod.getMethod().isAnnotationPresent(PostMapping.class)) {
            return extractPaths(handlerMethod.getMethod().getAnnotation(PostMapping.class).value(),
                    handlerMethod.getMethod().getAnnotation(PostMapping.class).path());
        }
        if (handlerMethod.getMethod().isAnnotationPresent(PatchMapping.class)) {
            return extractPaths(handlerMethod.getMethod().getAnnotation(PatchMapping.class).value(),
                    handlerMethod.getMethod().getAnnotation(PatchMapping.class).path());
        }
        if (handlerMethod.getMethod().isAnnotationPresent(DeleteMapping.class)) {
            return extractPaths(handlerMethod.getMethod().getAnnotation(DeleteMapping.class).value(),
                    handlerMethod.getMethod().getAnnotation(DeleteMapping.class).path());
        }
        RequestMapping requestMapping = handlerMethod.getMethod().getAnnotation(RequestMapping.class);
        if (requestMapping == null) {
            return Set.of("");
        }
        return extractPaths(requestMapping.value(), requestMapping.path());
    }

    private Set<String> extractPaths(RequestMapping requestMapping) {
        if (requestMapping == null) {
            return Set.of();
        }
        return extractPaths(requestMapping.value(), requestMapping.path());
    }

    private Set<String> extractPaths(String[] values, String[] paths) {
        String[] mappings = values.length > 0 ? values : paths;
        if (mappings.length == 0) {
            return Set.of("");
        }
        return new LinkedHashSet<>(Arrays.asList(mappings));
    }

    private String normalizePath(String path) {
        String normalized = path.replaceAll("/+", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
