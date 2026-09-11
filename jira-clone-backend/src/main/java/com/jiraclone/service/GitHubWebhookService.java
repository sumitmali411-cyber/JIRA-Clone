package com.jiraclone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiraclone.model.GitCommit;
import com.jiraclone.repository.IssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitHubWebhookService {

    private static final Pattern ISSUE_KEY_PATTERN =
            Pattern.compile("\\b([A-Z][A-Z0-9]+-\\d+)\\b");

    private static final ObjectMapper mapper = new ObjectMapper();

    @Value("${github.webhook.secret}")
    private String webhookSecret;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ActivityService activityService;

    public boolean isSignatureValid(String payload, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) return false;
        if (webhookSecret == null || webhookSecret.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = "sha256=" + HexFormat.of().formatHex(digest);
            // Constant-time: String.equals short-circuits on the first differing
            // character, which leaks the prefix length an attacker has guessed.
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public void processPushEvent(String payload) throws Exception {
        JsonNode root = mapper.readTree(payload);

        String ref = root.path("ref").asText("");
        String repoFullName = root.path("repository").path("full_name").asText("unknown");

        JsonNode commits = root.path("commits");
        if (!commits.isArray()) return;

        for (JsonNode commitNode : commits) {
            String sha = commitNode.path("id").asText();
            String message = commitNode.path("message").asText();
            String url = commitNode.path("url").asText();
            String authorName = commitNode.path("author").path("name").asText();
            String authorEmail = commitNode.path("author").path("email").asText();
            String timestamp = commitNode.path("timestamp").asText(Instant.now().toString());

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

            for (String key : extractIssueKeys(firstLine)) {
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
