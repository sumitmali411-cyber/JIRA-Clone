package com.jiraclone.model;

import com.jiraclone.model.enums.ProjectType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    private String id;
    private String name;
    private String key;
    private String description;
    private ProjectType type;
    private List<String> statuses;
    private Instant createdAt;
    private Instant updatedAt;
    private long issueCounter;
}
