package com.jiraclone.service;

import com.jiraclone.dto.IssueDto;
import com.jiraclone.dto.IssueLinkDto;
import com.jiraclone.model.Issue;
import com.jiraclone.model.IssueLink;
import com.jiraclone.model.enums.IssueType;
import com.jiraclone.model.enums.LinkType;
import com.jiraclone.model.enums.Priority;
import com.jiraclone.storage.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IssueService {

    @Autowired
    private DataStore dataStore;

    @Autowired
    @Lazy
    private ProjectService projectService;

    @Autowired
    private ActivityService activityService;

    public List<Issue> getByProjectId(String projectId) {
        return dataStore.readIssues().stream()
                .filter(i -> i.getProjectId().equals(projectId))
                .toList();
    }

    public Optional<Issue> getById(String id) {
        return dataStore.readIssues().stream()
                .filter(i -> i.getId().equals(id))
                .findFirst();
    }

    public Issue create(String projectId, IssueDto dto, String actor) {
        long num = projectService.nextIssueNumber(projectId);
        String projectKey = projectService.getById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found")).getKey();

        List<Issue> issues = new ArrayList<>(dataStore.readIssues());

        Issue issue = new Issue();
        issue.setId(UUID.randomUUID().toString());
        issue.setProjectId(projectId);
        issue.setIssueNumber(num);
        issue.setIssueKey(projectKey + "-" + num);
        issue.setType(dto.getType() != null ? dto.getType() : IssueType.TASK);
        issue.setSummary(dto.getSummary());
        issue.setDescription(dto.getDescription());
        issue.setStatus(dto.getStatus() != null ? dto.getStatus() : "To Do");
        issue.setPriority(dto.getPriority() != null ? dto.getPriority() : Priority.MEDIUM);
        issue.setAssignee(dto.getAssignee());
        issue.setReporter(dto.getReporter() != null ? dto.getReporter() : actor);
        issue.setSprintId(dto.getSprintId());
        issue.setEpicId(dto.getEpicId());
        issue.setParentId(dto.getParentId());
        issue.setLabels(dto.getLabels() != null ? dto.getLabels() : List.of());
        issue.setStoryPoints(dto.getStoryPoints());
        issue.setTimeEstimate(dto.getTimeEstimate());
        issue.setDueDate(dto.getDueDate());
        issue.setLinks(new ArrayList<>());
        issue.setCreatedAt(Instant.now());
        issue.setUpdatedAt(Instant.now());

        issues.add(issue);
        dataStore.writeIssues(issues);
        activityService.recordCreation(issue, actor);
        return issue;
    }

    public Issue update(String id, IssueDto dto, String actor) {
        List<Issue> issues = new ArrayList<>(dataStore.readIssues());
        Issue issue = issues.stream().filter(i -> i.getId().equals(id))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Issue not found: " + id));

        // Snapshot for activity
        Issue old = cloneIssue(issue);

        if (dto.getSummary() != null) issue.setSummary(dto.getSummary());
        if (dto.getDescription() != null) issue.setDescription(dto.getDescription());
        if (dto.getStatus() != null) issue.setStatus(dto.getStatus());
        if (dto.getPriority() != null) issue.setPriority(dto.getPriority());
        if (dto.getType() != null) issue.setType(dto.getType());
        if (dto.getAssignee() != null) issue.setAssignee(dto.getAssignee());
        if (dto.getReporter() != null) issue.setReporter(dto.getReporter());
        if (dto.getSprintId() != null) issue.setSprintId(dto.getSprintId());
        if (dto.getEpicId() != null) issue.setEpicId(dto.getEpicId());
        if (dto.getLabels() != null) issue.setLabels(dto.getLabels());
        if (dto.getStoryPoints() != null) issue.setStoryPoints(dto.getStoryPoints());
        if (dto.getTimeEstimate() != null) issue.setTimeEstimate(dto.getTimeEstimate());
        if (dto.getDueDate() != null) issue.setDueDate(dto.getDueDate());
        issue.setUpdatedAt(Instant.now());

        dataStore.writeIssues(issues);
        activityService.recordChanges(id, issue.getProjectId(), actor, old, issue);
        return issue;
    }

    public Issue updateStatus(String id, String status, String actor) {
        List<Issue> issues = new ArrayList<>(dataStore.readIssues());
        Issue issue = issues.stream().filter(i -> i.getId().equals(id))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Issue not found: " + id));
        String oldStatus = issue.getStatus();
        issue.setStatus(status);
        issue.setUpdatedAt(Instant.now());
        dataStore.writeIssues(issues);
        activityService.recordChange(id, issue.getProjectId(), actor, "status", oldStatus, status, "UPDATED");
        return issue;
    }

    public Issue updateSprint(String id, String sprintId, String actor) {
        List<Issue> issues = new ArrayList<>(dataStore.readIssues());
        Issue issue = issues.stream().filter(i -> i.getId().equals(id))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Issue not found: " + id));
        String oldSprintId = issue.getSprintId();
        issue.setSprintId(sprintId);
        issue.setUpdatedAt(Instant.now());
        dataStore.writeIssues(issues);
        activityService.recordChange(id, issue.getProjectId(), actor, "sprintId", oldSprintId, sprintId, "UPDATED");
        return issue;
    }

    public void delete(String id) {
        List<Issue> issues = new ArrayList<>(dataStore.readIssues());
        boolean removed = issues.removeIf(i -> i.getId().equals(id));
        if (!removed) throw new IllegalArgumentException("Issue not found: " + id);
        dataStore.writeIssues(issues);
    }

    public void deleteByProjectId(String projectId) {
        List<Issue> issues = new ArrayList<>(dataStore.readIssues());
        issues.removeIf(i -> i.getProjectId().equals(projectId));
        dataStore.writeIssues(issues);
    }

    public IssueLink addLink(String issueId, IssueLinkDto dto) {
        List<Issue> issues = new ArrayList<>(dataStore.readIssues());
        Issue source = issues.stream().filter(i -> i.getId().equals(issueId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));
        Issue target = issues.stream().filter(i -> i.getId().equals(dto.getTargetIssueId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Target issue not found: " + dto.getTargetIssueId()));

        IssueLink link = new IssueLink();
        link.setId(UUID.randomUUID().toString());
        link.setSourceIssueId(issueId);
        link.setTargetIssueId(target.getId());
        link.setTargetIssueKey(target.getIssueKey());
        link.setTargetIssueSummary(target.getSummary());
        link.setLinkType(dto.getLinkType());

        if (source.getLinks() == null) source.setLinks(new ArrayList<>());
        source.getLinks().add(link);
        source.setUpdatedAt(Instant.now());

        // Add reverse link if applicable
        LinkType reverseType = getReverseType(dto.getLinkType());
        if (reverseType != null) {
            IssueLink reverseLink = new IssueLink();
            reverseLink.setId(UUID.randomUUID().toString());
            reverseLink.setSourceIssueId(target.getId());
            reverseLink.setTargetIssueId(issueId);
            reverseLink.setTargetIssueKey(source.getIssueKey());
            reverseLink.setTargetIssueSummary(source.getSummary());
            reverseLink.setLinkType(reverseType);
            if (target.getLinks() == null) target.setLinks(new ArrayList<>());
            target.getLinks().add(reverseLink);
        }

        dataStore.writeIssues(issues);
        return link;
    }

    public void deleteLink(String issueId, String linkId) {
        List<Issue> issues = new ArrayList<>(dataStore.readIssues());
        Issue issue = issues.stream().filter(i -> i.getId().equals(issueId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));
        if (issue.getLinks() != null) {
            issue.getLinks().removeIf(l -> l.getId().equals(linkId));
        }
        issue.setUpdatedAt(Instant.now());
        dataStore.writeIssues(issues);
    }

    private LinkType getReverseType(LinkType type) {
        return switch (type) {
            case BLOCKS -> LinkType.IS_BLOCKED_BY;
            case IS_BLOCKED_BY -> LinkType.BLOCKS;
            case DUPLICATES -> LinkType.IS_DUPLICATED_BY;
            case IS_DUPLICATED_BY -> LinkType.DUPLICATES;
            default -> null; // RELATES_TO is symmetric, handled separately
        };
    }

    private Issue cloneIssue(Issue i) {
        Issue clone = new Issue();
        clone.setId(i.getId());
        clone.setProjectId(i.getProjectId());
        clone.setStatus(i.getStatus());
        clone.setSummary(i.getSummary());
        clone.setPriority(i.getPriority());
        clone.setType(i.getType());
        clone.setAssignee(i.getAssignee());
        clone.setSprintId(i.getSprintId());
        clone.setDueDate(i.getDueDate());
        clone.setStoryPoints(i.getStoryPoints());
        return clone;
    }
}
