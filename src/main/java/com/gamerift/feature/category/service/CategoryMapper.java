package com.gamerift.feature.category.service;

import com.gamerift.feature.category.dto.CategoryResponse;
import com.gamerift.feature.category.model.Category;

/** Copies data from the database model to the DTO. Plain static method, no magic. */
public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug(), category.getIcon());
    }
}
