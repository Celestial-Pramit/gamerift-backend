package com.gamerift.feature.admin.api;

import java.util.Map;

import com.gamerift.shared.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only area. Real admin endpoints (stats, user management...) arrive in Phase 9.
 * Today it only proves the role rules work.
 *
 * Two layers of protection: SecurityConfig says /api/admin/** needs ADMIN,
 * and @PreAuthorize repeats the rule on the class (defence in depth).
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin only (ADMIN role)")
public class AdminController {

    @GetMapping("/ping")
    @Operation(summary = "Check that you are an admin")
    public Map<String, String> ping(@AuthenticationPrincipal AuthUser admin) {
        return Map.of("message", "Hello admin", "email", admin.getEmail());
    }
}
