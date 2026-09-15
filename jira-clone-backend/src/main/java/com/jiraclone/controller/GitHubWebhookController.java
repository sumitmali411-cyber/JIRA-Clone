package com.jiraclone.controller;

import com.jiraclone.dto.ApiResponse;
import com.jiraclone.service.GitHubWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github")
public class GitHubWebhookController {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubWebhookController.class);

    @Autowired
    private GitHubWebhookService webhookService;

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<String>> webhook(
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "") String event,
            @RequestHeader(value = "X-Hub-Signature-256", defaultValue = "") String signature,
            @RequestBody String payload) {

        if ("ping".equals(event)) {
            return ResponseEntity.ok(ApiResponse.success("pong"));
        }

        if (!"push".equals(event)) {
            return ResponseEntity.ok(ApiResponse.success("ignored"));
        }

        if (!webhookService.isSignatureValid(payload, signature)) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid signature"));
        }

        try {
            webhookService.processPushEvent(payload);
            return ResponseEntity.ok(ApiResponse.success("processed"));
        } catch (Exception e) {
            LOG.error("Webhook processing failed", e);
            return ResponseEntity.status(500).body(ApiResponse.error("Processing failed"));
        }
    }
}
