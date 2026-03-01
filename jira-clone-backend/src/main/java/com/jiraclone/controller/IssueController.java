package com.jiraclone.controller;

import com.jiraclone.dto.*;
import com.jiraclone.model.Issue;
import com.jiraclone.model.IssueLink;
import com.jiraclone.service.IssueService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class IssueController {

    @Autowired
    private IssueService issueService;

    @GetMapping("/projects/{projectId}/issues")
    public ApiResponse<List<Issue>> listByProject(@PathVariable String projectId) {
        return ApiResponse.success(issueService.getByProjectId(projectId));
    }

    @PostMapping("/projects/{projectId}/issues")
    public ApiResponse<Issue> create(@PathVariable String projectId,
                                     @Valid @RequestBody IssueDto dto,
                                     @RequestParam(required = false) String actor) {
        return ApiResponse.success(issueService.create(projectId, dto, actor), "Issue created");
    }

    @GetMapping("/issues/{id}")
    public ResponseEntity<ApiResponse<Issue>> get(@PathVariable String id) {
        return issueService.getById(id)
                .map(i -> ResponseEntity.ok(ApiResponse.success(i)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/issues/{id}")
    public ResponseEntity<ApiResponse<Issue>> update(@PathVariable String id,
                                                      @RequestBody IssueDto dto,
                                                      @RequestParam(required = false) String actor) {
        return issueService.getById(id)
                .map(i -> ResponseEntity.ok(ApiResponse.success(issueService.update(id, dto, actor))))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/issues/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        issueService.delete(id);
        return ApiResponse.success(null, "Issue deleted");
    }

    @PatchMapping("/issues/{id}/status")
    public ApiResponse<Issue> updateStatus(@PathVariable String id,
                                            @Valid @RequestBody StatusUpdateDto dto) {
        return ApiResponse.success(issueService.updateStatus(id, dto.getStatus(), dto.getActor()));
    }

    @PatchMapping("/issues/{id}/sprint")
    public ApiResponse<Issue> updateSprint(@PathVariable String id,
                                            @RequestBody SprintAssignDto dto,
                                            @RequestParam(required = false) String actor) {
        return ApiResponse.success(issueService.updateSprint(id, dto.getSprintId(), actor));
    }

    @GetMapping("/issues/{id}/links")
    public ResponseEntity<ApiResponse<List<IssueLink>>> getLinks(@PathVariable String id) {
        return issueService.getById(id)
                .map(i -> ResponseEntity.ok(ApiResponse.success(i.getLinks() != null ? (List<IssueLink>) i.getLinks() : List.<IssueLink>of())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/issues/{id}/links")
    public ApiResponse<IssueLink> addLink(@PathVariable String id,
                                           @Valid @RequestBody IssueLinkDto dto) {
        return ApiResponse.success(issueService.addLink(id, dto), "Link added");
    }

    @DeleteMapping("/issues/{issueId}/links/{linkId}")
    public ApiResponse<Void> deleteLink(@PathVariable String issueId, @PathVariable String linkId) {
        issueService.deleteLink(issueId, linkId);
        return ApiResponse.success(null, "Link removed");
    }
}
