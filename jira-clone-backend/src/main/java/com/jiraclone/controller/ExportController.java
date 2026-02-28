package com.jiraclone.controller;

import com.jiraclone.dto.SearchFilterDto;
import com.jiraclone.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @GetMapping("/export/issues")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String label,
            @RequestParam(required = false) String sprintId) throws IOException {

        SearchFilterDto filter = new SearchFilterDto();
        filter.setQ(q);
        filter.setProjectId(projectId);
        filter.setType(type);
        filter.setStatus(status);
        filter.setPriority(priority);
        filter.setAssignee(assignee);
        filter.setLabel(label);
        filter.setSprintId(sprintId);

        byte[] data = exportService.exportToExcel(filter);
        String filename = "issues-" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(data);
    }
}
