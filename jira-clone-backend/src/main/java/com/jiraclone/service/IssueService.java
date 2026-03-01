package com.jiraclone.service;

import com.jiraclone.dto.IssueDto;
import com.jiraclone.dto.IssueLinkDto;
import com.jiraclone.model.Issue;
import com.jiraclone.model.IssueLink;
import com.jiraclone.model.enums.IssueType;
import com.jiraclone.model.enums.LinkType;
import com.jiraclone.model.enums.Priority;
import com.jiraclone.repository.IssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IssueService {

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    @Lazy
    private ProjectService projectService;

    @Autowired
    private ActivityService activityService;

    public List<Issue> getByProjectId(String projectId) {
        return issueRepository.findByProjectId(projectId);
    }

    public Optional<Issue> getById(String id) {
        return issueRepository.findById(id);
    }

    @Transactional
    public Issue create(String projectId, IssueDto dto, String actor) {
        long num = projectService.nextIssueNumber(projectId);
        String projectKey = projectService.getById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found")).getKey();

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
        issue.setLabels(dto.getLabels() != null ? dto.getLabels() : new ArrayList<>());
        issue.setStoryPoints(dto.getStoryPoints());
        issue.setTimeEstimate(dto.getTimeEstimate());
        issue.setDueDate(dto.getDueDate());
        issue.setLinks(new ArrayList<>());
        issue.setCreatedAt(Instant.now());
        issue.setUpdatedAt(Instant.now());

        Issue saved = issueRepository.save(issue);
        activityService.recordCreation(saved, actor);
        return saved;
    }

    @Transactional
    public Issue update(String id, IssueDto dto, String actor) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + id));

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

        Issue saved = issueRepository.save(issue);
        activityService.recordChanges(id, issue.getProjectId(), actor, old, saved);
        return saved;
    }

    @Transactional
    public Issue updateStatus(String id, String status, String actor) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + id));
        String oldStatus = issue.getStatus();
        issue.setStatus(status);
        issue.setUpdatedAt(Instant.now());
        Issue saved = issueRepository.save(issue);
        activityService.recordChange(id, issue.getProjectId(), actor, "status", oldStatus, status, "UPDATED");
        return saved;
    }

    @Transactional
    public Issue updateSprint(String id, String sprintId, String actor) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + id));
        String oldSprintId = issue.getSprintId();
        issue.setSprintId(sprintId);
        issue.setUpdatedAt(Instant.now());
        Issue saved = issueRepository.save(issue);
        activityService.recordChange(id, issue.getProjectId(), actor, "sprintId", oldSprintId, sprintId, "UPDATED");
        return saved;
    }

    @Transactional
    public void delete(String id) {
        if (!issueRepository.existsById(id)) {
            throw new IllegalArgumentException("Issue not found: " + id);
        }
        issueRepository.deleteById(id);
    }

    @Transactional
    public void deleteByProjectId(String projectId) {
        issueRepository.deleteByProjectId(projectId);
    }

    @Transactional
    public IssueLink addLink(String issueId, IssueLinkDto dto) {
        Issue source = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));
        Issue target = issueRepository.findById(dto.getTargetIssueId())
                .orElseThrow(() -> new IllegalArgumentException("Target issue not found: " + dto.getTargetIssueId()));

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
        issueRepository.save(source);

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
            issueRepository.save(target);
        }

        return link;
    }

    @Transactional
    public void deleteLink(String issueId, String linkId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + issueId));
        if (issue.getLinks() != null) {
            issue.getLinks().removeIf(l -> l.getId().equals(linkId));
        }
        issue.setUpdatedAt(Instant.now());
        issueRepository.save(issue);
    }

    private LinkType getReverseType(LinkType type) {
        return switch (type) {
            case BLOCKS -> LinkType.IS_BLOCKED_BY;
            case IS_BLOCKED_BY -> LinkType.BLOCKS;
            case DUPLICATES -> LinkType.IS_DUPLICATED_BY;
            case IS_DUPLICATED_BY -> LinkType.DUPLICATES;
            default -> null;
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
