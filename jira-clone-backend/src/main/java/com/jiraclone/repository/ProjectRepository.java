package com.jiraclone.repository;

import com.jiraclone.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
    boolean existsByKeyIgnoreCase(String key);
}
