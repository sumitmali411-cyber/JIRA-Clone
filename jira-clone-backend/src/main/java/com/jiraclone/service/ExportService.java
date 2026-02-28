package com.jiraclone.service;

import com.jiraclone.dto.SearchFilterDto;
import com.jiraclone.model.Issue;
import com.jiraclone.model.Sprint;
import com.jiraclone.storage.DataStore;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExportService {

    @Autowired
    private SearchService searchService;

    @Autowired
    private DataStore dataStore;

    private static final String[] HEADERS = {
            "Issue Key", "Type", "Summary", "Status", "Priority", "Assignee", "Reporter",
            "Sprint", "Epic", "Story Points", "Time Estimate", "Due Date", "Labels",
            "Created At", "Updated At", "Description"
    };

    public byte[] exportToExcel(SearchFilterDto filter) throws IOException {
        List<Issue> issues = searchService.search(filter);
        Map<String, String> sprintNames = dataStore.readSprints().stream()
                .collect(Collectors.toMap(Sprint::getId, Sprint::getName, (a, b) -> a));

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Issues");

            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Issue issue : issues) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(nvl(issue.getIssueKey()));
                row.createCell(1).setCellValue(issue.getType() != null ? issue.getType().name() : "");
                row.createCell(2).setCellValue(nvl(issue.getSummary()));
                row.createCell(3).setCellValue(nvl(issue.getStatus()));
                row.createCell(4).setCellValue(issue.getPriority() != null ? issue.getPriority().name() : "");
                row.createCell(5).setCellValue(nvl(issue.getAssignee()));
                row.createCell(6).setCellValue(nvl(issue.getReporter()));
                row.createCell(7).setCellValue(issue.getSprintId() != null
                        ? sprintNames.getOrDefault(issue.getSprintId(), issue.getSprintId()) : "");
                row.createCell(8).setCellValue(nvl(issue.getEpicId()));
                row.createCell(9).setCellValue(issue.getStoryPoints() != null ? issue.getStoryPoints().toString() : "");
                row.createCell(10).setCellValue(nvl(issue.getTimeEstimate()));
                row.createCell(11).setCellValue(nvl(issue.getDueDate()));
                row.createCell(12).setCellValue(issue.getLabels() != null
                        ? String.join(", ", issue.getLabels()) : "");
                row.createCell(13).setCellValue(issue.getCreatedAt() != null ? issue.getCreatedAt().toString() : "");
                row.createCell(14).setCellValue(issue.getUpdatedAt() != null ? issue.getUpdatedAt().toString() : "");
                row.createCell(15).setCellValue(stripHtml(issue.getDescription()));
            }

            // Auto-size columns
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }

    private String stripHtml(String html) {
        if (html == null || html.isBlank()) return "";
        return Jsoup.parse(html).text();
    }
}
