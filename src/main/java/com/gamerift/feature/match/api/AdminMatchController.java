package com.gamerift.feature.match.api;

import java.util.List;

import com.gamerift.feature.match.dto.MatchRequest;
import com.gamerift.feature.match.dto.MatchResponse;
import com.gamerift.feature.match.service.IMatchService;

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
 * Admin-only management of match cards (admin panel "Matches" screen).
 * Protected twice: SecurityConfig (/api/admin/** needs ADMIN) and @PreAuthorize here.
 * Request flow of "create": Browser -> AdminMatchController (@Valid) -> IMatchService
 *   -> MatchCardRepository -> MongoDB -> MatchResponse JSON (201).
 */
@RestController
@RequestMapping("/api/admin/matches")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Matches", description = "Create, update and delete match cards (ADMIN only)")
public class AdminMatchController {

    private final IMatchService matchService;

    public AdminMatchController(IMatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    @Operation(summary = "All match cards")
    public List<MatchResponse> getAll() {
        return matchService.getAll(null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a match card")
    public MatchResponse create(@Valid @RequestBody MatchRequest request) {
        return matchService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a match card")
    public MatchResponse update(@PathVariable String id, @Valid @RequestBody MatchRequest request) {
        return matchService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a match card")
    public void delete(@PathVariable String id) {
        matchService.delete(id);
    }
}
