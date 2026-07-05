package com.sula.secure_task_manager.manager.project.entity;
import com.sula.secure_task_manager.common.entity.BaseEntity;
import com.sula.secure_task_manager.manager.task.entity.Task;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "projects",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_projects_owner_name",
                columnNames = {"owner_id", "name"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Builder.Default
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();

    public void addTask(Task task) {
        tasks.add(task);
        task.setProject(this);
    }

    public void removeTask(Task task) {
        tasks.remove(task);
        task.setProject(null);
    }
}
