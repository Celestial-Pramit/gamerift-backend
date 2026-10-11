package com.gamerift.feature.category.api;

import com.gamerift.feature.category.dto.CategoryRequest;
import com.gamerift.feature.category.dto.CategoryResponse;
import com.gamerift.feature.category.service.ICategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only write operations for categories (the public reads are in CategoryController).
 * Protected twice: SecurityConfig (/api/admin/** needs ADMIN) and @PreAuthorize here (defence in depth).
 *
 * Request flow of "create": Browser -> AdminCategoryController (@Valid) -> ICategoryService
 *   -> CategoryRepository -> MongoDB -> CategoryResponse JSON (HTTP 201).
 */
@RestController
@RequestMapping("/api/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Categories", description = "Create, update and delete categories (ADMIN only)")
public class AdminCategoryController {

    private final ICategoryService categoryService;

    public AdminCategoryController(ICategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a category (409 if the slug is already used)")
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category")
    public CategoryResponse update(@PathVariable String id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a category (409 if streams use it)")
    public void delete(@PathVariable String id) {
        categoryService.delete(id);
    }
}
