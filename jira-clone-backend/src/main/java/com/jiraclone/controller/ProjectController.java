package com.jiraclone.controller;

import com.jiraclone.dto.ApiResponse;
import com.jiraclone.dto.ProjectDto;
import com.jiraclone.model.Project;
import com.jiraclone.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @GetMapping
    public ApiResponse<List<Project>> list() {
        return ApiResponse.success(projectService.getAll());
    }

    @PostMapping
    public ApiResponse<Project> create(@Valid @RequestBody ProjectDto dto) {
        return ApiResponse.success(projectService.create(dto), "Project created");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Project>> get(@PathVariable String id) {
        return projectService.getById(id)
                .map(p -> ResponseEntity.ok(ApiResponse.success(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Project>> update(@PathVariable String id, @RequestBody ProjectDto dto) {
        return projectService.getById(id)
                .map(p -> ResponseEntity.ok(ApiResponse.success(projectService.update(id, dto))))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        projectService.delete(id);
        return ApiResponse.success(null, "Project deleted");
    }
}
