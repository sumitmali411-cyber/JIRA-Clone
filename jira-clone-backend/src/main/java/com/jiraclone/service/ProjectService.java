package com.jiraclone.service;

import com.jiraclone.dto.ProjectDto;
import com.jiraclone.model.Project;
import com.jiraclone.model.enums.ProjectType;
import com.jiraclone.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueService issueService;

    @Autowired
    private SprintService sprintService;

    public List<Project> getAll() {
        return projectRepository.findAll();
    }

    public Optional<Project> getById(String id) {
        return projectRepository.findById(id);
    }

    @Transactional
    public Project create(ProjectDto dto) {
        if (projectRepository.existsByKeyIgnoreCase(dto.getKey())) {
            throw new IllegalArgumentException("Project key already exists: " + dto.getKey());
        }

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

        return projectRepository.save(p);
    }

    @Transactional
    public Project update(String id, ProjectDto dto) {
        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));

        if (dto.getName() != null) p.setName(dto.getName());
        if (dto.getDescription() != null) p.setDescription(dto.getDescription());
        if (dto.getType() != null) p.setType(dto.getType());
        if (dto.getStatuses() != null) p.setStatuses(dto.getStatuses());
        p.setUpdatedAt(Instant.now());

        return projectRepository.save(p);
    }

    @Transactional
    public void delete(String id) {
        if (!projectRepository.existsById(id)) {
            throw new IllegalArgumentException("Project not found: " + id);
        }
        projectRepository.deleteById(id);
        issueService.deleteByProjectId(id);
        sprintService.deleteByProjectId(id);
    }

    @Transactional
    public synchronized long nextIssueNumber(String projectId) {
        Project p = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        long next = p.getIssueCounter() + 1;
        p.setIssueCounter(next);
        p.setUpdatedAt(Instant.now());
        projectRepository.save(p);
        return next;
    }
}
