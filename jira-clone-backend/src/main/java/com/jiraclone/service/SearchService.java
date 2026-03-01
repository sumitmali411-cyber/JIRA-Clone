package com.jiraclone.service;

import com.jiraclone.dto.SearchFilterDto;
import com.jiraclone.model.Issue;
import com.jiraclone.repository.IssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {

    @Autowired
    private IssueRepository issueRepository;

    public List<Issue> search(SearchFilterDto filter) {
        String projectId = blank(filter.getProjectId());
        String type = blank(filter.getType());
        String status = blank(filter.getStatus());
        String priority = blank(filter.getPriority());
        String assignee = blank(filter.getAssignee());
        String sprintId = blank(filter.getSprintId());
        String q = blank(filter.getQ());

        List<Issue> results = issueRepository.search(projectId, type, status, priority, assignee, sprintId, q);

        // Filter by label in memory (JSON column not easily queryable)
        if (filter.getLabel() != null && !filter.getLabel().isBlank()) {
            String label = filter.getLabel();
            results = results.stream()
                    .filter(i -> i.getLabels() != null && i.getLabels().contains(label))
                    .toList();
        }

        return results;
    }

    private String blank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
