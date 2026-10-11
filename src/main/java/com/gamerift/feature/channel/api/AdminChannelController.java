package com.gamerift.feature.channel.api;

import java.util.List;

import com.gamerift.feature.channel.dto.AdminChannelResponse;
import com.gamerift.feature.channel.dto.ChannelRequest;
import com.gamerift.feature.channel.service.IAdminChannelService;

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
 * Admin-only management of channels (admin panel "Channels" screen).
 * Protected twice: SecurityConfig (/api/admin/** needs ADMIN) and @PreAuthorize here.
 * Request flow of "create": Browser -> AdminChannelController (@Valid) -> IAdminChannelService
 *   -> ChannelRepository (name check, save) -> MongoDB -> AdminChannelResponse JSON (201).
 */
@RestController
@RequestMapping("/api/admin/channels")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Channels", description = "Create, update and delete channels (ADMIN only)")
public class AdminChannelController {

    private final IAdminChannelService adminChannelService;

    public AdminChannelController(IAdminChannelService adminChannelService) {
        this.adminChannelService = adminChannelService;
    }

    @GetMapping
    @Operation(summary = "All channels")
    public List<AdminChannelResponse> getAll() {
        return adminChannelService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a channel")
    public AdminChannelResponse create(@Valid @RequestBody ChannelRequest request) {
        return adminChannelService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a channel")
    public AdminChannelResponse update(@PathVariable String id, @Valid @RequestBody ChannelRequest request) {
        return adminChannelService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a channel (only when it has no streams)")
    public void delete(@PathVariable String id) {
        adminChannelService.delete(id);
    }
}
