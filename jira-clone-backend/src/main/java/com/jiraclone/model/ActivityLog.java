package com.jiraclone.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {
    private String id;
    private String issueId;
    private String projectId;
    private String actor;
    private String field;
    private String oldValue;
    private String newValue;
    private String action;
    private Instant timestamp;
}
