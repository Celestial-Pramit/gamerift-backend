package com.gamerift.feature.payment.api;

import java.util.List;

import com.gamerift.feature.payment.dto.AdminOrderResponse;
import com.gamerift.feature.payment.service.IAdminOrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only view of every order (admin panel "Orders" screen). */
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Orders", description = "Every purchase with the buyer's username (ADMIN only)")
public class AdminOrderController {

    private final IAdminOrderService adminOrderService;

    public AdminOrderController(IAdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping
    @Operation(summary = "All orders, newest first")
    public List<AdminOrderResponse> getAll() {
        return adminOrderService.getAll();
    }
}