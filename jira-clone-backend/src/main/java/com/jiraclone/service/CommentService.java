package com.jiraclone.service;

import com.jiraclone.dto.CommentDto;
import com.jiraclone.model.Comment;
import com.jiraclone.storage.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CommentService {

    @Autowired
    private DataStore dataStore;

    public List<Comment> getByIssueId(String issueId) {
        return dataStore.readComments().stream()
                .filter(c -> c.getIssueId().equals(issueId))
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .toList();
    }

    public Comment create(String issueId, CommentDto dto) {
        List<Comment> comments = new ArrayList<>(dataStore.readComments());
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID().toString());
        comment.setIssueId(issueId);
        comment.setAuthor(dto.getAuthor());
        comment.setBody(dto.getBody());
        comment.setEdited(false);
        comment.setCreatedAt(Instant.now());
        comment.setUpdatedAt(Instant.now());
        comments.add(comment);
        dataStore.writeComments(comments);
        return comment;
    }

    public Comment update(String id, CommentDto dto) {
        List<Comment> comments = new ArrayList<>(dataStore.readComments());
        Comment comment = comments.stream().filter(c -> c.getId().equals(id))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Comment not found: " + id));
        if (dto.getBody() != null) comment.setBody(dto.getBody());
        comment.setEdited(true);
        comment.setUpdatedAt(Instant.now());
        dataStore.writeComments(comments);
        return comment;
    }

    public void delete(String id) {
        List<Comment> comments = new ArrayList<>(dataStore.readComments());
        boolean removed = comments.removeIf(c -> c.getId().equals(id));
        if (!removed) throw new IllegalArgumentException("Comment not found: " + id);
        dataStore.writeComments(comments);
    }
}
