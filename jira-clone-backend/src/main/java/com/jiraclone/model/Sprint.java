package com.jiraclone.model;

import com.jiraclone.model.enums.SprintStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "sprints")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sprint {
    @Id
    private String id;

    @Column(nullable = false)
    private String projectId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String goal;

    @Enumerated(EnumType.STRING)
    private SprintStatus status;

    private String startDate;
    private String endDate;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
