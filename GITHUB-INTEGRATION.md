# GitHub Integration — Issue-Linked Commits

## What This Does

GitHub sends a webhook event to your backend every time a commit is pushed.
Your backend parses the commit message for issue keys (e.g. `JC-1`, `PROJ-42`),
finds the matching issue, and stores the commit reference on that issue.
The frontend then shows all linked commits inside the issue detail page,
exactly like JIRA's "Development" panel.

Convention used in commit messages:

    git commit -m "JC-12 fix null pointer in sprint service"
    git commit -m "closes JC-12: finish sprint endpoint"
    git commit -m "refs JC-12 JC-15 refactor activity logging"

---

## Investigation Results

Searching the full codebase found zero GitHub-related files:

- No webhook endpoint exists
- No `GitCommit` model, entity, or POJO
- No reference to "github", "webhook", or commit parsing in any Java or TypeScript file
- The `ActivityLog` entity records field-level changes but has no `commitSha` or `source` field
- The `Issue` entity has a `links` column (stored as JSON TEXT via `IssueLinkListConverter`)
  but no `gitCommits` column

Everything below is a complete implementation guide starting from zero.

---

## How GitHub Webhooks Work

1. You register a webhook URL in your GitHub repository settings.
2. Every time commits are pushed, GitHub sends a POST request to that URL.
3. The request body is JSON containing the repository name, pusher, and an array of commits.
4. Each commit object has: `id` (sha), `message`, `url`, `author`, `timestamp`.
5. GitHub signs the payload with HMAC-SHA256 using a secret you configure.
   The signature is in the request header `X-Hub-Signature-256`.
6. Your backend validates the signature before trusting the payload.

Sample GitHub push payload (abbreviated):

    {
      "ref": "refs/heads/main",
      "repository": { "full_name": "yourname/jira-clone" },
      "pusher": { "name": "alice" },
      "commits": [
        {
          "id": "a1b2c3d4e5f6...",
          "message": "JC-12 fix null pointer in sprint service",
          "url": "https://github.com/yourname/jira-clone/commit/a1b2c3d4",
          "author": { "name": "Alice", "email": "alice@example.com" },
          "timestamp": "2026-03-01T10:00:00Z"
        }
      ]
    }

---

## Files to Create and Modify

### Summary

    Backend — NEW files
    ├── model/GitCommit.java                  POJO (stored as JSON inside Issue)
    ├── converter/GitCommitListConverter.java  JPA AttributeConverter (TEXT <-> List<GitCommit>)
    ├── controller/GitHubWebhookController.java  POST /api/github/webhook
    └── service/GitHubWebhookService.java     Parsing + issue linking logic

    Backend — MODIFY
    ├── model/Issue.java                      Add gitCommits field
    └── application.properties               Add github.webhook.secret

    Frontend — MODIFY
    ├── core/models/issue.model.ts            Add GitCommit + gitCommits field on Issue
    └── features/issue-detail/               Show commits section in template

---

## Step 1 — Backend: GitCommit POJO

Create `jira-clone-backend/src/main/java/com/jiraclone/model/GitCommit.java`:

```java
package com.jiraclone.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitCommit {
    private String sha;          // full commit hash
    private String shortSha;     // first 7 chars
    private String message;      // raw commit message (first line only)
    private String url;          // link to commit on GitHub
    private String authorName;
    private String authorEmail;
    private String timestamp;    // ISO-8601 string
    private String branch;       // e.g. "refs/heads/main"
    private String repoFullName; // e.g. "yourname/jira-clone"
}
```

---

## Step 2 — Backend: GitCommitListConverter

Create `jira-clone-backend/src/main/java/com/jiraclone/converter/GitCommitListConverter.java`:

```java
package com.jiraclone.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jiraclone.model.GitCommit;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class GitCommitListConverter implements AttributeConverter<List<GitCommit>, String> {

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public String convertToDatabaseColumn(List<GitCommit> attribute) {
        if (attribute == null || attribute.isEmpty()) return "[]";
        try {
            return mapper.writeValueAsString(attribute);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Override
    public List<GitCommit> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return new ArrayList<>();
        try {
            return mapper.readValue(dbData, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
```

---

## Step 3 — Backend: Modify Issue.java

Add the `gitCommits` field to the existing `Issue` entity.
Location: `jira-clone-backend/src/main/java/com/jiraclone/model/Issue.java`

Add these two imports at the top of the file (after existing imports):

```java
import com.jiraclone.converter.GitCommitListConverter;
import com.jiraclone.model.GitCommit;
```

Add this field to the class body (after the `links` field):

```java
@Convert(converter = GitCommitListConverter.class)
@Column(name = "git_commits", columnDefinition = "TEXT")
private List<GitCommit> gitCommits;
```

Hibernate's `ddl-auto=update` will add the `git_commits` column to the `issues` table automatically on next startup. No migration script needed.

---

## Step 4 — Backend: application.properties

Add to `jira-clone-backend/src/main/resources/application.properties`:

```properties
# GitHub Webhook secret — must match what you set in GitHub repo settings
github.webhook.secret=your-secret-here
```

---

## Step 5 — Backend: GitHubWebhookService

Create `jira-clone-backend/src/main/java/com/jiraclone/service/GitHubWebhookService.java`:

```java
package com.jiraclone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiraclone.model.GitCommit;
import com.jiraclone.model.Issue;
import com.jiraclone.repository.IssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitHubWebhookService {

    // Matches issue keys like JC-1, PROJ-42, MY-PROJECT-100
    private static final Pattern ISSUE_KEY_PATTERN =
            Pattern.compile("\\b([A-Z][A-Z0-9]+-\\d+)\\b");

    private static final ObjectMapper mapper = new ObjectMapper();

    @Value("${github.webhook.secret}")
    private String webhookSecret;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ActivityService activityService;

    /**
     * Validates the HMAC-SHA256 signature from GitHub.
     * GitHub sends: X-Hub-Signature-256: sha256=<hex>
     */
    public boolean isSignatureValid(String payload, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = "sha256=" + HexFormat.of().formatHex(digest);
            return expected.equals(signatureHeader);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Processes a GitHub push event payload.
     * Finds issue keys in commit messages, links commits to matching issues.
     */
    @Transactional
    public void processPushEvent(String payload) throws Exception {
        JsonNode root = mapper.readTree(payload);

        String ref = root.path("ref").asText("");           // "refs/heads/main"
        String repoFullName = root.path("repository")
                .path("full_name").asText("unknown");

        JsonNode commits = root.path("commits");
        if (!commits.isArray()) return;

        for (JsonNode commitNode : commits) {
            String sha = commitNode.path("id").asText();
            String message = commitNode.path("message").asText();
            String url = commitNode.path("url").asText();
            String authorName = commitNode.path("author").path("name").asText();
            String authorEmail = commitNode.path("author").path("email").asText();
            String timestamp = commitNode.path("timestamp").asText(Instant.now().toString());

            // Only use the first line of the commit message
            String firstLine = message.contains("\n") ? message.split("\n")[0] : message;

            GitCommit commit = new GitCommit(
                    sha,
                    sha.length() >= 7 ? sha.substring(0, 7) : sha,
                    firstLine,
                    url,
                    authorName,
                    authorEmail,
                    timestamp,
                    ref,
                    repoFullName
            );

            // Find all issue keys mentioned in the commit message
            List<String> mentionedKeys = extractIssueKeys(firstLine);
            for (String key : mentionedKeys) {
                linkCommitToIssue(key, commit);
            }
        }
    }

    private List<String> extractIssueKeys(String message) {
        List<String> keys = new ArrayList<>();
        Matcher m = ISSUE_KEY_PATTERN.matcher(message.toUpperCase());
        while (m.find()) {
            keys.add(m.group(1));
        }
        return keys;
    }

    private void linkCommitToIssue(String issueKey, GitCommit commit) {
        issueRepository.findByIssueKey(issueKey).ifPresent(issue -> {
            if (issue.getGitCommits() == null) {
                issue.setGitCommits(new ArrayList<>());
            }
            // Avoid duplicate commits (same SHA on the same issue)
            boolean alreadyLinked = issue.getGitCommits().stream()
                    .anyMatch(c -> c.getSha().equals(commit.getSha()));
            if (!alreadyLinked) {
                issue.getGitCommits().add(commit);
                issueRepository.save(issue);
                activityService.recordChange(
                        issue.getId(),
                        issue.getProjectId(),
                        commit.getAuthorName(),
                        "gitCommit",
                        null,
                        commit.getShortSha() + " " + commit.getMessage(),
                        "COMMIT_LINKED"
                );
            }
        });
    }
}
```

---

## Step 6 — Backend: Add findByIssueKey to IssueRepository

The service calls `issueRepository.findByIssueKey(issueKey)`.
Add this method to `IssueRepository.java`:

```java
import java.util.Optional;

Optional<Issue> findByIssueKey(String issueKey);
```

---

## Step 7 — Backend: GitHubWebhookController

Create `jira-clone-backend/src/main/java/com/jiraclone/controller/GitHubWebhookController.java`:

```java
package com.jiraclone.controller;

import com.jiraclone.dto.ApiResponse;
import com.jiraclone.service.GitHubWebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github")
public class GitHubWebhookController {

    @Autowired
    private GitHubWebhookService webhookService;

    /**
     * GitHub calls this endpoint on every push event.
     *
     * Headers sent by GitHub:
     *   X-GitHub-Event: push
     *   X-Hub-Signature-256: sha256=<hmac>
     *   Content-Type: application/json
     */
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<String>> webhook(
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "") String event,
            @RequestHeader(value = "X-Hub-Signature-256", defaultValue = "") String signature,
            @RequestBody String payload) {

        // Only process push events; ping events are sent when the webhook is created
        if ("ping".equals(event)) {
            return ResponseEntity.ok(ApiResponse.success("pong"));
        }

        if (!"push".equals(event)) {
            return ResponseEntity.ok(ApiResponse.success("ignored"));
        }

        if (!webhookService.isSignatureValid(payload, signature)) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Invalid signature"));
        }

        try {
            webhookService.processPushEvent(payload);
            return ResponseEntity.ok(ApiResponse.success("processed"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Processing failed: " + e.getMessage()));
        }
    }
}
```

---

## Step 8 — Backend: Permit the Webhook URL (No JWT Required)

GitHub cannot send a JWT token. The webhook endpoint must be publicly accessible without authentication.
Modify `SecurityConfig.java` to permit it:

```java
// Replace the existing authorizeHttpRequests block with:
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
    .requestMatchers(HttpMethod.POST, "/api/github/webhook").permitAll()  // add this line
    .anyRequest().authenticated()
)
```

---

## Step 9 — Frontend: Update issue.model.ts

Add `GitCommit` interface and `gitCommits` field to the `Issue` interface.
File: `jira-clone-frontend/src/app/core/models/issue.model.ts`

Add after the `IssueLink` interface:

```typescript
export interface GitCommit {
  sha: string;
  shortSha: string;
  message: string;
  url: string;
  authorName: string;
  authorEmail: string;
  timestamp: string;
  branch: string;
  repoFullName: string;
}
```

Add to the `Issue` interface after the `links` field:

```typescript
gitCommits: GitCommit[];
```

---

## Step 10 — Frontend: Show Commits in Issue Detail

In `issue-detail.component.ts`, import the `GitCommit` type:

```typescript
import { Issue, Priority, IssueType, GitCommit } from '../../core/models/issue.model';
```

Add a helper method to the component class:

```typescript
getBranchName(branch: string): string {
  // "refs/heads/main" -> "main"
  return branch.replace('refs/heads/', '');
}
```

In `issue-detail.component.html`, add the commits section below the comments section:

```html
<!-- Git Commits section — only shown when commits exist -->
<ng-container *ngIf="issue && issue.gitCommits && issue.gitCommits.length > 0">
  <p-divider />
  <h4 style="margin-bottom: 12px;">
    <i class="pi pi-github" style="margin-right: 6px;"></i>
    Linked Commits ({{ issue.gitCommits.length }})
  </h4>
  <div *ngFor="let commit of issue.gitCommits"
       style="display:flex; gap:12px; padding:10px 0; border-bottom:1px solid #e5e7eb;">
    <div style="flex:1;">
      <a [href]="commit.url" target="_blank" style="font-family:monospace; font-weight:600; color:#4a9eed;">
        {{ commit.shortSha }}
      </a>
      <span style="margin-left:8px; color:#374151;">{{ commit.message }}</span>
      <div style="font-size:12px; color:#6b7280; margin-top:4px;">
        <i class="pi pi-user" style="margin-right:4px;"></i>{{ commit.authorName }}
        &nbsp;·&nbsp;
        <i class="pi pi-code-branch" style="margin-right:4px;"></i>{{ getBranchName(commit.branch) }}
        &nbsp;·&nbsp;
        {{ commit.timestamp | date:'medium' }}
      </div>
    </div>
  </div>
</ng-container>
```

---

## Step 11 — Register the Webhook in GitHub

1. Open your GitHub repository in a browser.
2. Go to **Settings** → **Webhooks** → **Add webhook**.
3. Fill in:

   - **Payload URL**: `http://your-server:8080/api/github/webhook`
   - **Content type**: `application/json`
   - **Secret**: the same value you put in `application.properties` under `github.webhook.secret`
   - **Which events**: select "Just the push event"
   - **Active**: checked

4. Click **Add webhook**.

GitHub will immediately send a `ping` event to verify the endpoint is reachable.
Your controller returns `"pong"` for ping events — GitHub shows a green checkmark.

---

## Local Development: Expose localhost to GitHub

GitHub needs a public URL to send webhooks. For local development, use ngrok:

```bash
# Install ngrok from https://ngrok.com/download

# Expose your backend port 8080
ngrok http 8080

# ngrok gives you a URL like:
# https://abc123.ngrok-free.app

# Use this as your webhook URL:
# https://abc123.ngrok-free.app/api/github/webhook
```

Note: ngrok URLs change every time you restart ngrok (on the free plan).
Update the webhook URL in GitHub settings each time.

---

## Commit Message Convention

Your team should follow this pattern for commits to be linked automatically:

    # Reference by issue key anywhere in the message
    git commit -m "JC-12 fix login redirect after token expiry"

    # Multiple issues in one commit
    git commit -m "JC-12 JC-15 refactor auth interceptor"

    # Keywords are ignored (regex only looks for the key pattern)
    git commit -m "closes JC-12 fix null pointer in sprint service"
    git commit -m "refs JC-12: update sprint endpoint"

The regex `[A-Z][A-Z0-9]+-\d+` matches any uppercase project key followed by a dash and number.
Matching is case-insensitive (the service upcases the commit message before matching).

---

## How Activity Log Works After Integration

When a commit is linked, `ActivityService.recordChange` is called with:
- `field`: `"gitCommit"`
- `action`: `"COMMIT_LINKED"`
- `newValue`: `"a1b2c3d JC-12 fix null pointer"`

This entry appears in the issue's activity feed alongside field changes like
status updates and priority changes, giving a unified history.

---

## Full Data Flow Summary

    Developer pushes commit with message "JC-12 fix sprint endpoint"
            |
            v
    GitHub sends POST to /api/github/webhook
    Headers: X-GitHub-Event: push
             X-Hub-Signature-256: sha256=<hmac>
            |
            v
    GitHubWebhookController receives request
    → validates HMAC signature against github.webhook.secret
    → calls GitHubWebhookService.processPushEvent(payload)
            |
            v
    GitHubWebhookService parses each commit
    → extracts "JC-12" from commit message using regex
    → calls IssueRepository.findByIssueKey("JC-12")
    → appends GitCommit to issue.gitCommits list
    → saves issue (Hibernate serializes gitCommits -> JSON TEXT)
    → calls ActivityService.recordChange(..., "COMMIT_LINKED")
            |
            v
    User opens issue JC-12 in the browser
    → IssueService.getById returns issue with gitCommits populated
    → Angular renders commits section with SHA link, message, author, branch, time

---

## Troubleshooting

**GitHub shows "invalid signature" (401)**
- The `github.webhook.secret` in `application.properties` must match exactly what you entered in GitHub
- Check for trailing spaces in both places
- Restart Spring Boot after changing `application.properties`

**Webhook delivers successfully but commits don't appear on issue**
- Check the issue key exists in the database (`issueKey` column) and matches the commit message exactly
- Issue keys are case-sensitive after the uppercase conversion. Use uppercase in commit messages.
- Check Spring Boot logs for exceptions in `GitHubWebhookService`

**"Connection refused" shown in GitHub delivery log**
- Spring Boot is not running, or the webhook URL points to a port that isn't open
- For local dev, verify ngrok is running and pointing to port 8080

**`git_commits` column not created in MySQL**
- Restart Spring Boot. `ddl-auto=update` adds columns on startup.
- Verify the `@Convert` and `@Column` annotations are on the field.

**Ping succeeds but push events fail**
- Ensure the push event is checked in GitHub webhook settings (not just ping)

---

## Optional Enhancements

**Automatic status transition**

If the commit message starts with `fixes`, `closes`, or `resolves`, automatically move the issue to "Done":

In `GitHubWebhookService.linkCommitToIssue`, after saving the commit:

```java
String lower = commit.getMessage().toLowerCase();
if (lower.startsWith("fixes ") || lower.startsWith("closes ") || lower.startsWith("resolves ")) {
    issue.setStatus("Done");
    activityService.recordChange(issue.getId(), issue.getProjectId(),
            commit.getAuthorName(), "status", issue.getStatus(), "Done", "UPDATED");
}
```

**Per-project GitHub repo binding**

Add a `githubRepo` field to the `Project` entity so you can restrict which repository's
commits are linked to which project:

```java
// In Project.java
private String githubRepo;  // e.g. "yourname/jira-clone"
```

In `linkCommitToIssue`, only link if `issue's project.githubRepo == commit.repoFullName`.
