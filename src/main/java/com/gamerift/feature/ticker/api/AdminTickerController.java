package com.gamerift.feature.ticker.api;

import java.util.List;

import com.gamerift.feature.ticker.dto.TickerMessageRequest;
import com.gamerift.feature.ticker.dto.TickerMessageResponse;
import com.gamerift.feature.ticker.service.ITickerService;

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
 * Admin-only management of ticker messages (admin panel "Ticker" screen).
 * Protected twice: SecurityConfig (/api/admin/** needs ADMIN) and @PreAuthorize here.
 */
@RestController
@RequestMapping("/api/admin/ticker")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Ticker", description = "Create, update and delete ticker messages (ADMIN only)")
public class AdminTickerController {

    private final ITickerService tickerService;

    public AdminTickerController(ITickerService tickerService) {
        this.tickerService = tickerService;
    }

    @GetMapping
    @Operation(summary = "All ticker messages including inactive ones")
    public List<TickerMessageResponse> getAll() {
        return tickerService.getAllMessages();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a ticker message")
    public TickerMessageResponse create(@Valid @RequestBody TickerMessageRequest request) {
        return tickerService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a ticker message (text or active on/off)")
    public TickerMessageResponse update(@PathVariable String id, @Valid @RequestBody TickerMessageRequest request) {
        return tickerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a ticker message")
    public void delete(@PathVariable String id) {
        tickerService.delete(id);
    }
}
