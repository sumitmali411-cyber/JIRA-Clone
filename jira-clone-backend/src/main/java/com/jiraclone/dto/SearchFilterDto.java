package com.jiraclone.dto;

import lombok.Data;

@Data
public class SearchFilterDto {
    private String q;
    private String projectId;
    private String type;
    private String status;
    private String priority;
    private String assignee;
    private String label;
    private String sprintId;
}
