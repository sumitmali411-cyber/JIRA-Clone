package com.jiraclone.service;

import com.jiraclone.dto.SearchFilterDto;
import com.jiraclone.model.Issue;
import com.jiraclone.storage.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class SearchService {

    @Autowired
    private DataStore dataStore;

    public List<Issue> search(SearchFilterDto filter) {
        Stream<Issue> stream = dataStore.readIssues().stream();

        if (filter.getProjectId() != null && !filter.getProjectId().isBlank()) {
            stream = stream.filter(i -> filter.getProjectId().equals(i.getProjectId()));
        }
        if (filter.getType() != null && !filter.getType().isBlank()) {
            stream = stream.filter(i -> i.getType() != null && filter.getType().equalsIgnoreCase(i.getType().name()));
        }
        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            stream = stream.filter(i -> filter.getStatus().equalsIgnoreCase(i.getStatus()));
        }
        if (filter.getPriority() != null && !filter.getPriority().isBlank()) {
            stream = stream.filter(i -> i.getPriority() != null && filter.getPriority().equalsIgnoreCase(i.getPriority().name()));
        }
        if (filter.getAssignee() != null && !filter.getAssignee().isBlank()) {
            stream = stream.filter(i -> filter.getAssignee().equalsIgnoreCase(i.getAssignee()));
        }
        if (filter.getLabel() != null && !filter.getLabel().isBlank()) {
            stream = stream.filter(i -> i.getLabels() != null && i.getLabels().contains(filter.getLabel()));
        }
        if (filter.getSprintId() != null && !filter.getSprintId().isBlank()) {
            stream = stream.filter(i -> filter.getSprintId().equals(i.getSprintId()));
        }
        if (filter.getQ() != null && !filter.getQ().isBlank()) {
            String q = filter.getQ().toLowerCase();
            stream = stream.filter(i ->
                    (i.getSummary() != null && i.getSummary().toLowerCase().contains(q))
                    || (i.getIssueKey() != null && i.getIssueKey().toLowerCase().contains(q))
                    || (i.getDescription() != null && i.getDescription().toLowerCase().contains(q))
            );
        }

        return stream.toList();
    }
}
