package com.jiraclone.controller;

import com.jiraclone.dto.ApiResponse;
import com.jiraclone.model.ActivityLog;
import com.jiraclone.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @GetMapping("/issues/{issueId}/activity")
    public ApiResponse<List<ActivityLog>> list(@PathVariable String issueId) {
        return ApiResponse.success(activityService.getByIssueId(issueId));
    }
}
