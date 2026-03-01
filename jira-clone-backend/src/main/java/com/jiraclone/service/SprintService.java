package com.jiraclone.service;

import com.jiraclone.dto.SprintDto;
import com.jiraclone.model.Sprint;
import com.jiraclone.model.enums.SprintStatus;
import com.jiraclone.repository.SprintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SprintService {

    @Autowired
    private SprintRepository sprintRepository;

    public List<Sprint> getByProjectId(String projectId) {
        return sprintRepository.findByProjectId(projectId);
    }

    public Optional<Sprint> getById(String id) {
        return sprintRepository.findById(id);
    }

    @Transactional
    public Sprint create(String projectId, SprintDto dto) {
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
        return sprintRepository.save(sprint);
    }

    @Transactional
    public Sprint update(String id, SprintDto dto) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sprint not found: " + id));
        if (dto.getName() != null) sprint.setName(dto.getName());
        if (dto.getGoal() != null) sprint.setGoal(dto.getGoal());
        if (dto.getStartDate() != null) sprint.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) sprint.setEndDate(dto.getEndDate());
        sprint.setUpdatedAt(Instant.now());
        return sprintRepository.save(sprint);
    }

    @Transactional
    public Sprint start(String id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sprint not found: " + id));
        if (sprintRepository.existsByProjectIdAndStatusAndIdNot(sprint.getProjectId(), SprintStatus.ACTIVE, id)) {
            throw new IllegalStateException("Another sprint is already active in this project");
        }
        sprint.setStatus(SprintStatus.ACTIVE);
        sprint.setUpdatedAt(Instant.now());
        return sprintRepository.save(sprint);
    }

    @Transactional
    public Sprint complete(String id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sprint not found: " + id));
        sprint.setStatus(SprintStatus.COMPLETED);
        sprint.setCompletedAt(Instant.now());
        sprint.setUpdatedAt(Instant.now());
        return sprintRepository.save(sprint);
    }

    @Transactional
    public void delete(String id) {
        if (!sprintRepository.existsById(id)) {
            throw new IllegalArgumentException("Sprint not found: " + id);
        }
        sprintRepository.deleteById(id);
    }

    @Transactional
    public void deleteByProjectId(String projectId) {
        sprintRepository.deleteByProjectId(projectId);
    }
}
