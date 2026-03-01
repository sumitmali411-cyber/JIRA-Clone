package com.jiraclone.model;

import com.jiraclone.converter.StringListConverter;
import com.jiraclone.model.enums.ProjectType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "project_key", nullable = false, unique = true)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private ProjectType type;

    @Convert(converter = StringListConverter.class)
    @Column(name = "statuses", columnDefinition = "TEXT")
    private List<String> statuses;

    private Instant createdAt;
    private Instant updatedAt;
    private long issueCounter;
}
