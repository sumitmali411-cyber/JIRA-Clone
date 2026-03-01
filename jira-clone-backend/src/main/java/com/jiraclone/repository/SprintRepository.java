package com.jiraclone.repository;

import com.jiraclone.model.Sprint;
import com.jiraclone.model.enums.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, String> {
    List<Sprint> findByProjectId(String projectId);

    void deleteByProjectId(String projectId);

    boolean existsByProjectIdAndStatusAndIdNot(String projectId, SprintStatus status, String id);
}
