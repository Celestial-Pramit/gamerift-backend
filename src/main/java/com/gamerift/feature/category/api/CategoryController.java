package com.gamerift.feature.category.api;

import java.util.List;

import com.gamerift.feature.category.dto.CategoryResponse;
import com.gamerift.feature.category.service.ICategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller (layer: HTTP). Receives the request, calls the service, returns the DTO as JSON.
 * It never touches the repository and never contains business rules.
 *
 * Request flow:  Browser -> CategoryController -> ICategoryService (CategoryServiceImpl)
 *                -> CategoryRepository -> MongoDB -> back up as CategoryResponse DTOs.
 *
 * @RestController = @Controller + @ResponseBody (return values become JSON).
 * @RequestMapping sets the common URL prefix. Phase 8 adds the admin create/update/delete here.
 */
@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Game categories (Phase 5 sample feature)")
public class CategoryController {

    private final ICategoryService categoryService;

    // Constructor injection: we depend on the INTERFACE
    public CategoryController(ICategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "List all categories")
    public List<CategoryResponse> getAll() {
        return categoryService.getAll();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get one category by slug (404 if it does not exist)")
    public CategoryResponse getBySlug(@PathVariable String slug) {
        return categoryService.getBySlug(slug);
    }
}
