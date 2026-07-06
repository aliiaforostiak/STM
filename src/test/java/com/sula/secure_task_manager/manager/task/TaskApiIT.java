package com.sula.secure_task_manager.manager.task;

import com.jayway.jsonpath.JsonPath;
import com.sula.secure_task_manager.IntegrationTestSupport;
import com.sula.secure_task_manager.manager.project.entity.Project;
import com.sula.secure_task_manager.manager.project.repository.ProjectRepository;
import com.sula.secure_task_manager.manager.task.dto.TaskCreateRequest;
import com.sula.secure_task_manager.manager.task.dto.TaskPriority;
import com.sula.secure_task_manager.manager.task.dto.TaskStatus;
import com.sula.secure_task_manager.manager.task.dto.TaskUpdateRequest;
import com.sula.secure_task_manager.manager.task.entity.Task;
import com.sula.secure_task_manager.manager.task.repository.TaskRepository;
import com.sula.secure_task_manager.manager.user.User;
import com.sula.secure_task_manager.manager.user.UserRepository;
import com.sula.secure_task_manager.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskApiIT extends IntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldCreateAndReadTaskThroughProtectedApi() throws Exception {
        User owner = userRepository.save(User.create("owner@example.com", "encoded-password"));
        User assignee = userRepository.save(User.create("assignee@example.com", "encoded-password"));
        Project project = projectRepository.save(Project.builder()
                .name("Alpha")
                .description("Alpha project")
                .ownerId(owner.getId())
                .build());

        String token = jwtService.generateAccessToken(owner);

        String createBody = objectMapper.writeValueAsString(new TaskCreateRequest(
                "Implement JWT login",
                "Add token generation and login endpoint",
                project.getId(),
                assignee.getId(),
                TaskPriority.HIGH,
                Instant.parse("2026-07-10T10:00:00Z")
        ));

        String responseBody = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Implement JWT login"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.project.id").value(project.getId()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long taskId = ((Number) JsonPath.read(responseBody, "$.id")).longValue();

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.creator.id").value(owner.getId()))
                .andExpect(jsonPath("$.assignee.id").value(assignee.getId()));
    }

    @Test
    void shouldReturnForbiddenForTaskFromForeignProject() throws Exception {
        User owner = userRepository.save(User.create("owner@example.com", "encoded-password"));
        User foreignUser = userRepository.save(User.create("foreign@example.com", "encoded-password"));
        Project project = projectRepository.save(Project.builder()
                .name("Alpha")
                .ownerId(owner.getId())
                .build());
        Task task = taskRepository.save(Task.builder()
                .title("Foreign task")
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.TODO)
                .project(project)
                .creatorId(owner.getId())
                .build());

        String token = jwtService.generateAccessToken(foreignUser);

        mockMvc.perform(get("/api/tasks/{id}", task.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void shouldRejectInvalidTaskStatusTransition() throws Exception {
        User owner = userRepository.save(User.create("owner@example.com", "encoded-password"));
        Project project = projectRepository.save(Project.builder()
                .name("Alpha")
                .ownerId(owner.getId())
                .build());
        Task task = taskRepository.save(Task.builder()
                .title("Invalid transition task")
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.TODO)
                .project(project)
                .creatorId(owner.getId())
                .build());

        String token = jwtService.generateAccessToken(owner);

        String updateBody = objectMapper.writeValueAsString(new TaskUpdateRequest(
                null,
                null,
                null,
                TaskStatus.DONE,
                null,
                null
        ));

        mockMvc.perform(patch("/api/tasks/{id}", task.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.details[0].field").value("status"));
    }

    @Test
    void shouldDeleteTaskAndReturnProjectTasks() throws Exception {
        User owner = userRepository.save(User.create("owner@example.com", "encoded-password"));
        Project project = projectRepository.save(Project.builder()
                .name("Alpha")
                .ownerId(owner.getId())
                .build());

        Task firstTask = taskRepository.save(Task.builder()
                .title("First task")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.TODO)
                .project(project)
                .creatorId(owner.getId())
                .build());

        taskRepository.save(Task.builder()
                .title("Second task")
                .priority(TaskPriority.LOW)
                .status(TaskStatus.IN_PROGRESS)
                .project(project)
                .creatorId(owner.getId())
                .build());

        String token = jwtService.generateAccessToken(owner);

        mockMvc.perform(get("/api/projects/{projectId}/tasks", project.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(delete("/api/tasks/{id}", firstTask.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{projectId}/tasks", project.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Second task"));
    }
}
