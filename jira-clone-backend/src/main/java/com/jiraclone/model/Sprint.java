package com.jiraclone.model;

import com.jiraclone.model.enums.SprintStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sprint {
    private String id;
    private String projectId;
    private String name;
    private String goal;
    private SprintStatus status;
    private String startDate;
    private String endDate;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
