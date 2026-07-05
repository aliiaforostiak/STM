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
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void findAllByProjectId_shouldReturnOnlyProjectTasks() {
        Project firstProject = projectRepository.save(project("Secure Task Manager", 1L));
        Project secondProject = projectRepository.save(project("Another Project", 1L));

        Task first = task(firstProject, 7L, 8L, TaskStatus.TODO);
        Task second = task(firstProject, 7L, null, TaskStatus.IN_PROGRESS);
        Task third = task(secondProject, 7L, 8L, TaskStatus.DONE);

        taskRepository.saveAll(List.of(first, second, third));

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
    void findAllByProjectId_shouldReturnEmptyList_whenProjectHasNoTasks() {
        List<Task> result = taskRepository.findAllByProject_Id(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void save_shouldSetDefaultStatusAndAuditFields() {
        Project project = projectRepository.save(project("Secure Task Manager", 1L));

        Task task = Task.builder()
                .title("Implement JWT login")
                .description("Add access token generation and login endpoint")
                .priority(TaskPriority.HIGH)
                .project(project)
                .creatorId(7L)
                .assigneeId(8L)
                .dueDate(Instant.parse("2026-07-10T10:00:00Z"))
                .build();

        Task savedTask = taskRepository.saveAndFlush(task);

        assertThat(savedTask.getId()).isNotNull();
        assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(savedTask.getCreatedAt()).isNotNull();
        assertThat(savedTask.getUpdatedAt()).isNotNull();
    }

    private Task task(Project project, Long creatorId, Long assigneeId, TaskStatus status) {
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

    private Project project(String name, Long ownerId) {
        return Project.builder()
                .name(name)
                .ownerId(ownerId)
                .build();
    }
}
