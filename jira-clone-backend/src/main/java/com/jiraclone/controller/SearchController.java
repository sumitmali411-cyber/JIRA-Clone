package com.jiraclone.controller;

import com.jiraclone.dto.ApiResponse;
import com.jiraclone.dto.SearchFilterDto;
import com.jiraclone.model.Issue;
import com.jiraclone.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("/search")
    public ApiResponse<List<Issue>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String label,
            @RequestParam(required = false) String sprintId) {

        SearchFilterDto filter = new SearchFilterDto();
        filter.setQ(q);
        filter.setProjectId(projectId);
        filter.setType(type);
        filter.setStatus(status);
        filter.setPriority(priority);
        filter.setAssignee(assignee);
        filter.setLabel(label);
        filter.setSprintId(sprintId);

        return ApiResponse.success(searchService.search(filter));
    }
}
