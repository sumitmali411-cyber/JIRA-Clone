package com.jiraclone.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    private String id;

    @Column(nullable = false)
    private String issueId;

    private String author;

    @Column(columnDefinition = "TEXT")
    private String body;

    private Instant createdAt;
    private Instant updatedAt;
    private boolean edited;
}
