package com.gamerift.feature.comment.api;

import java.util.List;

import com.gamerift.feature.comment.dto.AdminCommentResponse;
import com.gamerift.feature.comment.service.IAdminCommentService;
import com.gamerift.feature.stream.dto.ToggleRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only management of comments (admin panel "Comments" screen). Protected twice, like every admin screen. */
@RestController
@RequestMapping("/api/admin/comments")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Comments", description = "List, delete, pin and lock comments (ADMIN only)")
public class AdminCommentController {

    private final IAdminCommentService adminCommentService;

    public AdminCommentController(IAdminCommentService adminCommentService) {
        this.adminCommentService = adminCommentService;
    }

    @GetMapping
    @Operation(summary = "All comments, newest first")
    public List<AdminCommentResponse> getAll() {
        return adminCommentService.getAll();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete any comment (replies and likes are removed too)")
    public void delete(@PathVariable String id) {
        adminCommentService.delete(id);
    }

    @PutMapping("/{id}/pin")
    @Operation(summary = "Pin or unpin a comment")
    public AdminCommentResponse pin(@PathVariable String id, @RequestBody ToggleRequest request) {
        return adminCommentService.setPinned(id, request.isEnabled());
    }

    @PutMapping("/{id}/lock")
    @Operation(summary = "Lock or unlock a comment")
    public AdminCommentResponse lock(@PathVariable String id, @RequestBody ToggleRequest request) {
        return adminCommentService.setLocked(id, request.isEnabled());
    }
}