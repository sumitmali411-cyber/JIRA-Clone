package com.jiraclone.dto;

import com.jiraclone.model.enums.IssueType;
import com.jiraclone.model.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class IssueDto {
    @NotBlank
    private String summary;

    private IssueType type;
    private String status;
    private Priority priority;
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
}
