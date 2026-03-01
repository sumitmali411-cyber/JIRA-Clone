package com.jiraclone.model;

import com.jiraclone.model.enums.LinkType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueLink {
    private String id;
    private String sourceIssueId;
    private String targetIssueId;
    private String targetIssueKey;
    private String targetIssueSummary;
    private LinkType linkType;
}
