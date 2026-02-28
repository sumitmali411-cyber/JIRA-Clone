package com.jiraclone.model;

import com.jiraclone.model.enums.IssueType;
import com.jiraclone.model.enums.Priority;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Issue {
    private String id;
    private String projectId;
    private String issueKey;
    private long issueNumber;
    private IssueType type;
    private String status;
    private Priority priority;
    private String summary;
    private String description;
    private String assignee;
    private String reporter;
    private String sprintId;
    private String epicId;
    private String parentId;
    private List<String> labels;
    private Integer storyPoints;
    private String timeEstimate;
    private String dueDate;
    private Instant createdAt;
    private Instant updatedAt;
    private List<IssueLink> links;
}
