package com.jiraclone.controller;

import com.jiraclone.dto.ApiResponse;
import com.jiraclone.dto.CommentDto;
import com.jiraclone.model.Comment;
import com.jiraclone.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping("/issues/{issueId}/comments")
    public ApiResponse<List<Comment>> list(@PathVariable String issueId) {
        return ApiResponse.success(commentService.getByIssueId(issueId));
    }

    @PostMapping("/issues/{issueId}/comments")
    public ApiResponse<Comment> create(@PathVariable String issueId, @Valid @RequestBody CommentDto dto) {
        return ApiResponse.success(commentService.create(issueId, dto), "Comment added");
    }

    @PutMapping("/comments/{id}")
    public ApiResponse<Comment> update(@PathVariable String id, @RequestBody CommentDto dto) {
        return ApiResponse.success(commentService.update(id, dto));
    }

    @DeleteMapping("/comments/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        commentService.delete(id);
        return ApiResponse.success(null, "Comment deleted");
    }
}
