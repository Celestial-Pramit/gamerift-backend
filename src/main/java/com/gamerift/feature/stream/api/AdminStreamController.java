package com.gamerift.feature.stream.api;

import java.util.List;

import com.gamerift.feature.stream.dto.StreamRequest;
import com.gamerift.feature.stream.dto.StreamResponse;
import com.gamerift.feature.stream.dto.ToggleRequest;
import com.gamerift.feature.stream.service.IAdminStreamService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only management of streams (the admin panel table and stream form).
 * Protected twice: SecurityConfig (/api/admin/** needs ADMIN) and @PreAuthorize here.
 *
 * Request flow of "create": Browser -> AdminStreamController (@Valid) -> IAdminStreamService
 *   -> CategoryRepository + ChannelRepository (checks) -> LiveStreamRepository -> MongoDB -> StreamResponse JSON (201).
 */
@RestController
@RequestMapping("/api/admin/streams")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Streams", description = "Create, update, delete and switch streams (ADMIN only)")
public class AdminStreamController {

    private final IAdminStreamService adminStreamService;

    public AdminStreamController(IAdminStreamService adminStreamService) {
        this.adminStreamService = adminStreamService;
    }

    @GetMapping
    @Operation(summary = "All streams including drafts")
    public List<StreamResponse> getAll() {
        return adminStreamService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a stream")
    public StreamResponse create(@Valid @RequestBody StreamRequest request) {
        return adminStreamService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a stream")
    public StreamResponse update(@PathVariable String id, @Valid @RequestBody StreamRequest request) {
        return adminStreamService.update(id, request);
    }

    @PutMapping("/{id}/live")
    @Operation(summary = "Switch 'Live now' on or off")
    public StreamResponse setLive(@PathVariable String id, @RequestBody ToggleRequest request) {
        return adminStreamService.setLive(id, request.isEnabled());
    }

    @PutMapping("/{id}/published")
    @Operation(summary = "Switch 'Published' on or off (off = draft)")
    public StreamResponse setPublished(@PathVariable String id, @RequestBody ToggleRequest request) {
        return adminStreamService.setPublished(id, request.isEnabled());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a stream")
    public void delete(@PathVariable String id) {
        adminStreamService.delete(id);
    }
}
