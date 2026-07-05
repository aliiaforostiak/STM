package com.sula.secure_task_manager.manager.project.repository;

import com.sula.secure_task_manager.common.config.JpaAuditingConfig;
import com.sula.secure_task_manager.manager.project.entity.Project;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ProjectRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void findAllByOwnerId_shouldReturnOnlyOwnerProjects() {
        Project first = Project.builder()
                .name("Secure Task Manager")
                .description("Backend project")
                .ownerId(1L)
                .build();

        Project second = Project.builder()
                .name("Notification Service")
                .description("Email service")
                .ownerId(1L)
                .build();


        Project third = Project.builder()
                .name("Bonus Service")
                .description("Clients supporting service")
                .ownerId(2L)
                .build();

        projectRepository.saveAll(List.of(first, second, third));

        List<Project> result = projectRepository.findAllByOwnerId(1L);

        assertThat(result)
                .hasSize(2)
                .extracting(Project::getName)
                .containsExactlyInAnyOrder("Secure Task Manager", "Notification Service");

    }

    @Test
    void existsByOwnerIdAndName_shouldReturnTrue_whenProjectExistsForOwner() {
        Project project = Project.builder()
                .name("Secure Task Manager")
                .description("Backend project")
                .ownerId(1L)
                .build();

        projectRepository.save(project);

        boolean exists = projectRepository.existsByOwnerIdAndName(
                1L,
                "Secure Task Manager"
        );

        assertThat(exists).isTrue();
    }

    @Test
    void existsByOwnerIdAndName_shouldReturnFalse_whenProjectExistsForAnotherOwner() {
        Project project = Project.builder()
                .name("Secure Task Manager")
                .description("Backend project")
                .ownerId(2L)
                .build();

        projectRepository.save(project);

        boolean exists = projectRepository.existsByOwnerIdAndName(
                1L,
                "Secure Task Manager"
        );

        assertThat(exists).isFalse();
    }

}
