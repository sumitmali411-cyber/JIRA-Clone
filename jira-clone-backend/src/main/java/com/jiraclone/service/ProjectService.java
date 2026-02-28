package com.jiraclone.service;

import com.jiraclone.dto.ProjectDto;
import com.jiraclone.model.Project;
import com.jiraclone.model.enums.ProjectType;
import com.jiraclone.storage.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProjectService {

    @Autowired
    private DataStore dataStore;

    @Autowired
    private IssueService issueService;

    @Autowired
    private SprintService sprintService;

    public List<Project> getAll() {
        return dataStore.readProjects();
    }

    public Optional<Project> getById(String id) {
        return dataStore.readProjects().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
    }

    public Project create(ProjectDto dto) {
        List<Project> projects = new ArrayList<>(dataStore.readProjects());

        // Check unique key
        boolean keyExists = projects.stream().anyMatch(p -> p.getKey().equalsIgnoreCase(dto.getKey()));
        if (keyExists) throw new IllegalArgumentException("Project key already exists: " + dto.getKey());

        Project p = new Project();
        p.setId(UUID.randomUUID().toString());
        p.setName(dto.getName());
        p.setKey(dto.getKey().toUpperCase());
        p.setDescription(dto.getDescription());
        p.setType(dto.getType() != null ? dto.getType() : ProjectType.SCRUM);
        p.setStatuses(dto.getStatuses() != null ? dto.getStatuses()
                : List.of("To Do", "In Progress", "In Review", "Done"));
        p.setIssueCounter(0);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());

        projects.add(p);
        dataStore.writeProjects(projects);
        return p;
    }

    public Project update(String id, ProjectDto dto) {
        List<Project> projects = new ArrayList<>(dataStore.readProjects());
        Project p = projects.stream().filter(x -> x.getId().equals(id))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));

        if (dto.getName() != null) p.setName(dto.getName());
        if (dto.getDescription() != null) p.setDescription(dto.getDescription());
        if (dto.getType() != null) p.setType(dto.getType());
        if (dto.getStatuses() != null) p.setStatuses(dto.getStatuses());
        p.setUpdatedAt(Instant.now());

        dataStore.writeProjects(projects);
        return p;
    }

    public void delete(String id) {
        List<Project> projects = new ArrayList<>(dataStore.readProjects());
        boolean removed = projects.removeIf(p -> p.getId().equals(id));
        if (!removed) throw new IllegalArgumentException("Project not found: " + id);

        dataStore.writeProjects(projects);
        // Cascade: delete issues and sprints
        issueService.deleteByProjectId(id);
        sprintService.deleteByProjectId(id);
    }

    // Used internally to increment issue counter
    public synchronized long nextIssueNumber(String projectId) {
        List<Project> projects = new ArrayList<>(dataStore.readProjects());
        Project p = projects.stream().filter(x -> x.getId().equals(projectId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        long next = p.getIssueCounter() + 1;
        p.setIssueCounter(next);
        p.setUpdatedAt(Instant.now());
        dataStore.writeProjects(projects);
        return next;
    }
}
