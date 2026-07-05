package com.sula.secure_task_manager.manager.task.entity;

import com.sula.secure_task_manager.common.entity.BaseEntity;
import com.sula.secure_task_manager.manager.task.dto.TaskPriority;
import com.sula.secure_task_manager.manager.task.dto.TaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "tasks",
        indexes = {
                @Index(name = "idx_tasks_project_id", columnList = "project_id"),
                @Index(name = "idx_tasks_creator_id", columnList = "creator_id"),
                @Index(name = "idx_tasks_assignee_id", columnList = "assignee_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void onCreate() {
        if (status == null) {
            status = TaskStatus.TODO;
        }
    }
}
