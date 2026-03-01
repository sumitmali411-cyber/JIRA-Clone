package com.jiraclone.model;

import com.jiraclone.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Entity
@Table(name = "app_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppConfig {
    @Id
    private Long id = 1L;

    @Convert(converter = StringListConverter.class)
    @Column(name = "labels", columnDefinition = "TEXT")
    private List<String> labels;

    private String defaultAssignee;
    private String defaultReporter;

    @Convert(converter = StringListConverter.class)
    @Column(name = "team_members", columnDefinition = "TEXT")
    private List<String> teamMembers;
}
