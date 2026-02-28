package com.jiraclone.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppConfig {
    private List<String> labels;
    private String defaultAssignee;
    private String defaultReporter;
    private List<String> teamMembers;
}
