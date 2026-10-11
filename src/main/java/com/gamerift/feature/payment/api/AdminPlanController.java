package com.gamerift.feature.payment.api;

import java.util.List;

import com.gamerift.feature.payment.dto.PlanRequest;
import com.gamerift.feature.payment.dto.PlanResponse;
import com.gamerift.feature.payment.service.IAdminPlanService;

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

/** Admin-only management of Pro plans (admin panel "Plans" screen). */
@RestController
@RequestMapping("/api/admin/plans")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Plans", description = "Create / edit / delete Pro plans (ADMIN only)")
public class AdminPlanController {

    private final IAdminPlanService adminPlanService;

    public AdminPlanController(IAdminPlanService adminPlanService) {
        this.adminPlanService = adminPlanService;
    }

    @GetMapping
    @Operation(summary = "All plans")
    public List<PlanResponse> getAll() {
        return adminPlanService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a plan")
    public PlanResponse create(@Valid @RequestBody PlanRequest request) {
        return adminPlanService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a plan")
    public PlanResponse update(@PathVariable String id, @Valid @RequestBody PlanRequest request) {
        return adminPlanService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a plan")
    public void delete(@PathVariable String id) {
        adminPlanService.delete(id);
    }
}