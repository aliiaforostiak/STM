package com.sula.secure_task_manager.manager.project.repository;

import com.sula.secure_task_manager.manager.project.entity.Project;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProjectRepositoryPostgresTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRejectDuplicateProjectNameForSameOwner() {
        long ownerId = insertUser("owner@example.com");

        projectRepository.save(Project.builder()
                .name("Alpha")
                .description("First project")
                .ownerId(ownerId)
                .build());

        Project duplicate = Project.builder()
                .name("Alpha")
                .description("Duplicate project")
                .ownerId(ownerId)
                .build();

        assertThatThrownBy(() -> projectRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowSameProjectNameForDifferentOwners() {
        long firstOwnerId = insertUser("owner1@example.com");
        long secondOwnerId = insertUser("owner2@example.com");

        projectRepository.saveAndFlush(Project.builder()
                .name("Alpha")
                .description("First owner project")
                .ownerId(firstOwnerId)
                .build());

        Project savedProject = projectRepository.saveAndFlush(Project.builder()
                .name("Alpha")
                .description("Second owner project")
                .ownerId(secondOwnerId)
                .build());

        assertThat(savedProject.getId()).isNotNull();
        assertThat(projectRepository.findAllByOwnerId(secondOwnerId))
                .extracting(Project::getName)
                .containsExactly("Alpha");
    }

    @Test
    void shouldRejectProjectWithMissingOwner() {
        Project project = Project.builder()
                .name("Alpha")
                .description("Orphan project")
                .ownerId(999_999L)
                .build();

        assertThatThrownBy(() -> projectRepository.saveAndFlush(project))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long insertUser(String email) {
        Instant now = Instant.now();

        Long id = jdbcTemplate.queryForObject("""
            INSERT INTO users (email, password, role, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
            """,
                Long.class,
                email,
                "encoded-password",
                "USER",
                Timestamp.from(now),
                Timestamp.from(now)
        );

        assertThat(id).isNotNull();

        return id;
    }
}
