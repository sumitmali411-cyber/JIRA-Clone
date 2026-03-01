package com.jiraclone.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentDto {
    @NotBlank
    private String author;

    @NotBlank
    private String body;
}
