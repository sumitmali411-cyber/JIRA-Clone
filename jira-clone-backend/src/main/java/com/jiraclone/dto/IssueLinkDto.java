package com.jiraclone.dto;

import com.jiraclone.model.enums.LinkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IssueLinkDto {
    @NotBlank
    private String targetIssueId;

    @NotNull
    private LinkType linkType;
}
