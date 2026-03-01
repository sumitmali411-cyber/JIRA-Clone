package com.jiraclone.repository;

import com.jiraclone.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, String> {
    List<ActivityLog> findByIssueIdOrderByTimestampDesc(String issueId);
}
