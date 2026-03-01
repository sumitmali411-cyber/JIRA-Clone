package com.jiraclone.service;

import com.jiraclone.dto.CommentDto;
import com.jiraclone.model.Comment;
import com.jiraclone.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    public List<Comment> getByIssueId(String issueId) {
        return commentRepository.findByIssueIdOrderByCreatedAtAsc(issueId);
    }

    @Transactional
    public Comment create(String issueId, CommentDto dto) {
        Comment comment = new Comment();
        comment.setId(UUID.randomUUID().toString());
        comment.setIssueId(issueId);
        comment.setAuthor(dto.getAuthor());
        comment.setBody(dto.getBody());
        comment.setEdited(false);
        comment.setCreatedAt(Instant.now());
        comment.setUpdatedAt(Instant.now());
        return commentRepository.save(comment);
    }

    @Transactional
    public Comment update(String id, CommentDto dto) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + id));
        if (dto.getBody() != null) comment.setBody(dto.getBody());
        comment.setEdited(true);
        comment.setUpdatedAt(Instant.now());
        return commentRepository.save(comment);
    }

    @Transactional
    public void delete(String id) {
        if (!commentRepository.existsById(id)) {
            throw new IllegalArgumentException("Comment not found: " + id);
        }
        commentRepository.deleteById(id);
    }
}
