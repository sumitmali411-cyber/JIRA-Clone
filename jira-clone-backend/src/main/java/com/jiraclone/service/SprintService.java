package com.jiraclone.service;

import com.jiraclone.dto.SprintDto;
import com.jiraclone.model.Sprint;
import com.jiraclone.model.enums.SprintStatus;
import com.jiraclone.storage.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SprintService {

    @Autowired
    private DataStore dataStore;

    public List<Sprint> getByProjectId(String projectId) {
        return dataStore.readSprints().stream()
                .filter(s -> s.getProjectId().equals(projectId))
                .toList();
    }

    public Optional<Sprint> getById(String id) {
        return dataStore.readSprints().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst();
    }

    public Sprint create(String projectId, SprintDto dto) {
        List<Sprint> sprints = new ArrayList<>(dataStore.readSprints());
        Sprint sprint = new Sprint();
        sprint.setId(UUID.randomUUID().toString());
        sprint.setProjectId(projectId);
        sprint.setName(dto.getName());
        sprint.setGoal(dto.getGoal());
        sprint.setStatus(SprintStatus.PLANNED);
        sprint.setStartDate(dto.getStartDate());
        sprint.setEndDate(dto.getEndDate());
        sprint.setCreatedAt(Instant.now());
        sprint.setUpdatedAt(Instant.now());
        sprints.add(sprint);
        dataStore.writeSprints(sprints);
        return sprint;
    }

    public Sprint update(String id, SprintDto dto) {
        List<Sprint> sprints = new ArrayList<>(dataStore.readSprints());
        Sprint sprint = sprints.stream().filter(s -> s.getId().equals(id))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Sprint not found: " + id));
        if (dto.getName() != null) sprint.setName(dto.getName());
        if (dto.getGoal() != null) sprint.setGoal(dto.getGoal());
        if (dto.getStartDate() != null) sprint.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) sprint.setEndDate(dto.getEndDate());
        sprint.setUpdatedAt(Instant.now());
        dataStore.writeSprints(sprints);
        return sprint;
    }

    public Sprint start(String id) {
        List<Sprint> sprints = new ArrayList<>(dataStore.readSprints());
        // Check no other active sprint in same project
        Sprint sprint = sprints.stream().filter(s -> s.getId().equals(id))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Sprint not found: " + id));
        boolean hasActive = sprints.stream()
                .anyMatch(s -> s.getProjectId().equals(sprint.getProjectId())
                        && s.getStatus() == SprintStatus.ACTIVE
                        && !s.getId().equals(id));
        if (hasActive) throw new IllegalStateException("Another sprint is already active in this project");
        sprint.setStatus(SprintStatus.ACTIVE);
        sprint.setUpdatedAt(Instant.now());
        dataStore.writeSprints(sprints);
        return sprint;
    }

    public Sprint complete(String id) {
        List<Sprint> sprints = new ArrayList<>(dataStore.readSprints());
        Sprint sprint = sprints.stream().filter(s -> s.getId().equals(id))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Sprint not found: " + id));
        sprint.setStatus(SprintStatus.COMPLETED);
        sprint.setCompletedAt(Instant.now());
        sprint.setUpdatedAt(Instant.now());
        dataStore.writeSprints(sprints);
        return sprint;
    }

    public void delete(String id) {
        List<Sprint> sprints = new ArrayList<>(dataStore.readSprints());
        boolean removed = sprints.removeIf(s -> s.getId().equals(id));
        if (!removed) throw new IllegalArgumentException("Sprint not found: " + id);
        dataStore.writeSprints(sprints);
    }

    public void deleteByProjectId(String projectId) {
        List<Sprint> sprints = new ArrayList<>(dataStore.readSprints());
        sprints.removeIf(s -> s.getProjectId().equals(projectId));
        dataStore.writeSprints(sprints);
    }
}
