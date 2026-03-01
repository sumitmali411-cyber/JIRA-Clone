package com.jiraclone.model;

import com.jiraclone.converter.IssueLinkListConverter;
import com.jiraclone.converter.StringListConverter;
import com.jiraclone.model.enums.IssueType;
import com.jiraclone.model.enums.Priority;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "issues")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Issue {
    @Id
    private String id;

    @Column(nullable = false)
    private String projectId;

    private String issueKey;
    private long issueNumber;

    @Enumerated(EnumType.STRING)
    private IssueType type;

    private String status;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Column(nullable = false)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String assignee;
    private String reporter;
    private String sprintId;
    private String epicId;
    private String parentId;

    @Convert(converter = StringListConverter.class)
    @Column(name = "labels", columnDefinition = "TEXT")
    private List<String> labels;

    private Integer storyPoints;
    private String timeEstimate;
    private String dueDate;
    private Instant createdAt;
    private Instant updatedAt;

    @Convert(converter = IssueLinkListConverter.class)
    @Column(name = "links", columnDefinition = "TEXT")
    private List<IssueLink> links;
}
