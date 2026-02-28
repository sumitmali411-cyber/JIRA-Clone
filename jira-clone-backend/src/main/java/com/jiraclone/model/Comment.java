package com.jiraclone.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    private String id;
    private String issueId;
    private String author;
    private String body;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean edited;
}
