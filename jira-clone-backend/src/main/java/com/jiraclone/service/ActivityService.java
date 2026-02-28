package com.jiraclone.service;

import com.jiraclone.model.ActivityLog;
import com.jiraclone.model.Issue;
import com.jiraclone.storage.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ActivityService {

    @Autowired
    private DataStore dataStore;

    public List<ActivityLog> getByIssueId(String issueId) {
        return dataStore.readActivity().stream()
                .filter(a -> a.getIssueId().equals(issueId))
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .toList();
    }

    public void recordCreation(Issue issue, String actor) {
        recordChange(issue.getId(), issue.getProjectId(), actor, "issue", null, "created", "CREATED");
    }

    public void recordChanges(String issueId, String projectId, String actor, Issue oldIssue, Issue newIssue) {
        checkField(issueId, projectId, actor, "status", oldIssue.getStatus(), newIssue.getStatus());
        checkField(issueId, projectId, actor, "summary", oldIssue.getSummary(), newIssue.getSummary());
        checkField(issueId, projectId, actor, "priority",
                oldIssue.getPriority() != null ? oldIssue.getPriority().name() : null,
                newIssue.getPriority() != null ? newIssue.getPriority().name() : null);
        checkField(issueId, projectId, actor, "assignee", oldIssue.getAssignee(), newIssue.getAssignee());
        checkField(issueId, projectId, actor, "type",
                oldIssue.getType() != null ? oldIssue.getType().name() : null,
                newIssue.getType() != null ? newIssue.getType().name() : null);
        checkField(issueId, projectId, actor, "sprintId", oldIssue.getSprintId(), newIssue.getSprintId());
        checkField(issueId, projectId, actor, "dueDate", oldIssue.getDueDate(), newIssue.getDueDate());
        checkField(issueId, projectId, actor, "storyPoints",
                oldIssue.getStoryPoints() != null ? oldIssue.getStoryPoints().toString() : null,
                newIssue.getStoryPoints() != null ? newIssue.getStoryPoints().toString() : null);
    }

    private void checkField(String issueId, String projectId, String actor,
                             String field, String oldVal, String newVal) {
        if (!Objects.equals(oldVal, newVal)) {
            recordChange(issueId, projectId, actor, field, oldVal, newVal, "UPDATED");
        }
    }

    public void recordChange(String issueId, String projectId, String actor,
                              String field, String oldValue, String newValue, String action) {
        List<ActivityLog> logs = new ArrayList<>(dataStore.readActivity());
        ActivityLog log = new ActivityLog();
        log.setId(UUID.randomUUID().toString());
        log.setIssueId(issueId);
        log.setProjectId(projectId);
        log.setActor(actor != null ? actor : "System");
        log.setField(field);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setAction(action);
        log.setTimestamp(Instant.now());
        logs.add(log);
        dataStore.writeActivity(logs);
    }
}
