package com.sula.secure_task_manager.manager.entity;

import com.sula.secure_task_manager.manager.dto.project.ProjectShortResponse;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private Long ownerId;

    private Instant createdAt;

    private Instant updatedAt;

}
