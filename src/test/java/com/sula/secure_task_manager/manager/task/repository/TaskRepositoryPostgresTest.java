package com.sula.secure_task_manager.manager.task.repository;

import com.sula.secure_task_manager.common.config.JpaAuditingConfig;
import com.sula.secure_task_manager.manager.project.entity.Project;
import com.sula.secure_task_manager.manager.project.repository.ProjectRepository;
import com.sula.secure_task_manager.manager.task.dto.TaskPriority;
import com.sula.secure_task_manager.manager.task.dto.TaskStatus;
import com.sula.secure_task_manager.manager.task.entity.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@Import(JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskRepositoryPostgresTest {

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
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveTaskWithProjectRelationAndAuditFields() {
        long ownerId = insertUser("owner@example.com");
        long assigneeId = insertUser("assignee@example.com");
        Project project = projectRepository.saveAndFlush(project("Alpha", ownerId));

        Task savedTask = taskRepository.saveAndFlush(task(project, ownerId, assigneeId, TaskStatus.TODO));

        assertThat(savedTask.getId()).isNotNull();
        assertThat(savedTask.getProject().getId()).isEqualTo(project.getId());
        assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(savedTask.getCreatedAt()).isNotNull();
        assertThat(savedTask.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldReturnOnlyTasksForGivenProject() {
        long ownerId = insertUser("owner@example.com");
        long assigneeId = insertUser("assignee@example.com");
        Project firstProject = projectRepository.saveAndFlush(project("Alpha", ownerId));
        Project secondProject = projectRepository.saveAndFlush(project("Beta", ownerId));

        taskRepository.saveAllAndFlush(List.of(
                task(firstProject, ownerId, assigneeId, TaskStatus.TODO),
                task(firstProject, ownerId, null, TaskStatus.IN_PROGRESS),
                task(secondProject, ownerId, assigneeId, TaskStatus.DONE)
        ));

        List<Task> result = taskRepository.findAllByProject_Id(firstProject.getId());

        assertThat(result)
                .hasSize(2)
                .extracting(task -> task.getProject().getId(), Task::getStatus)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(firstProject.getId(), TaskStatus.TODO),
                        org.assertj.core.groups.Tuple.tuple(firstProject.getId(), TaskStatus.IN_PROGRESS)
                );
    }

    @Test
    void shouldRejectTaskWithMissingProject() {
        long creatorId = insertUser("creator@example.com");
        long assigneeId = insertUser("assignee@example.com");

        Task invalidTask = Task.builder()
                .title("Broken task")
                .description("Task with missing project")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.TODO)
                .project(Project.builder().id(999_999L).name("Ghost").ownerId(creatorId).build())
                .creatorId(creatorId)
                .assigneeId(assigneeId)
                .dueDate(Instant.parse("2026-07-10T10:00:00Z"))
                .build();

        assertThatThrownBy(() -> taskRepository.saveAndFlush(invalidTask))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldDeleteProjectTasksWhenProjectIsRemoved() {
        long ownerId = insertUser("owner@example.com");
        long assigneeId = insertUser("assignee@example.com");
        Project project = projectRepository.saveAndFlush(project("Alpha", ownerId));

        taskRepository.saveAndFlush(task(project, ownerId, assigneeId, TaskStatus.TODO));
        taskRepository.saveAndFlush(task(project, ownerId, null, TaskStatus.IN_PROGRESS));

        jdbcTemplate.update("DELETE FROM projects WHERE id = ?", project.getId());

        assertThat(taskRepository.findAllByProject_Id(project.getId())).isEmpty();
    }

    private Project project(String name, long ownerId) {
        return Project.builder()
                .name(name)
                .description(name + " description")
                .ownerId(ownerId)
                .build();
    }

    private Task task(Project project, long creatorId, Long assigneeId, TaskStatus status) {
        return Task.builder()
                .title("Implement JWT login")
                .description("Add access token generation and login endpoint")
                .priority(TaskPriority.HIGH)
                .status(status)
                .project(project)
                .creatorId(creatorId)
                .assigneeId(assigneeId)
                .dueDate(Instant.parse("2026-07-10T10:00:00Z"))
                .build();
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
