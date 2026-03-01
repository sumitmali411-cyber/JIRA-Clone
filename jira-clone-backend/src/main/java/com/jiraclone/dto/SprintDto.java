package com.jiraclone.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SprintDto {
    @NotBlank
    private String name;

    private String goal;
    private String startDate;
    private String endDate;
}
