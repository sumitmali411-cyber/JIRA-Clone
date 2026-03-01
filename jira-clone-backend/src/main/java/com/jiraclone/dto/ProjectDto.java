package com.jiraclone.dto;

import com.jiraclone.model.enums.ProjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProjectDto {
    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "[A-Z]{2,10}", message = "Key must be 2-10 uppercase letters")
    private String key;

    private String description;

    private ProjectType type;

    @Size(min = 1)
    private List<String> statuses;
}
