package com.jiraclone.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitCommit {
    private String sha;
    private String shortSha;
    private String message;
    private String url;
    private String authorName;
    private String authorEmail;
    private String timestamp;
    private String branch;
    private String repoFullName;
}
