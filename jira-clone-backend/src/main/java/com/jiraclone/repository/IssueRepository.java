package com.jiraclone.repository;

import com.jiraclone.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IssueRepository extends JpaRepository<Issue, String> {
    List<Issue> findByProjectId(String projectId);

    Optional<Issue> findByIssueKey(String issueKey);

    void deleteByProjectId(String projectId);

    @Query("SELECT i FROM Issue i WHERE " +
           "(:projectId IS NULL OR i.projectId = :projectId) AND " +
           "(:type IS NULL OR UPPER(i.type) = UPPER(:type)) AND " +
           "(:status IS NULL OR UPPER(i.status) = UPPER(:status)) AND " +
           "(:priority IS NULL OR UPPER(i.priority) = UPPER(:priority)) AND " +
           "(:assignee IS NULL OR UPPER(i.assignee) = UPPER(:assignee)) AND " +
           "(:sprintId IS NULL OR i.sprintId = :sprintId) AND " +
           "(:q IS NULL OR LOWER(i.summary) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "   OR LOWER(i.issueKey) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "   OR LOWER(i.description) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<Issue> search(
            @Param("projectId") String projectId,
            @Param("type") String type,
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("assignee") String assignee,
            @Param("sprintId") String sprintId,
            @Param("q") String q
    );
}
