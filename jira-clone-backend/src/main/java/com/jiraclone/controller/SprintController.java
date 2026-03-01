package com.jiraclone.controller;

import com.jiraclone.dto.ApiResponse;
import com.jiraclone.dto.SprintDto;
import com.jiraclone.model.Sprint;
import com.jiraclone.service.SprintService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SprintController {

    @Autowired
    private SprintService sprintService;

    @GetMapping("/projects/{projectId}/sprints")
    public ApiResponse<List<Sprint>> list(@PathVariable String projectId) {
        return ApiResponse.success(sprintService.getByProjectId(projectId));
    }

    @PostMapping("/projects/{projectId}/sprints")
    public ApiResponse<Sprint> create(@PathVariable String projectId, @Valid @RequestBody SprintDto dto) {
        return ApiResponse.success(sprintService.create(projectId, dto), "Sprint created");
    }

    @GetMapping("/sprints/{id}")
    public ResponseEntity<ApiResponse<Sprint>> get(@PathVariable String id) {
        return sprintService.getById(id)
                .map(s -> ResponseEntity.ok(ApiResponse.success(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/sprints/{id}")
    public ResponseEntity<ApiResponse<Sprint>> update(@PathVariable String id, @RequestBody SprintDto dto) {
        return sprintService.getById(id)
                .map(s -> ResponseEntity.ok(ApiResponse.success(sprintService.update(id, dto))))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/sprints/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        sprintService.delete(id);
        return ApiResponse.success(null, "Sprint deleted");
    }

    @PostMapping("/sprints/{id}/start")
    public ApiResponse<Sprint> start(@PathVariable String id) {
        return ApiResponse.success(sprintService.start(id), "Sprint started");
    }

    @PostMapping("/sprints/{id}/complete")
    public ApiResponse<Sprint> complete(@PathVariable String id) {
        return ApiResponse.success(sprintService.complete(id), "Sprint completed");
    }
}
