package com.gamerift.feature.category.service;

import java.util.List;

import com.gamerift.feature.category.dto.CategoryRequest;
import com.gamerift.feature.category.dto.CategoryResponse;

/**
 * Service INTERFACE: describes WHAT the category feature can do.
 * The controllers depend on this interface, not on the implementation
 * (Dependency Inversion + Interface Segregation from SOLID).
 */
public interface ICategoryService {

    List<CategoryResponse> getAll();

    CategoryResponse getBySlug(String slug);

    /** Admin: creates a category. The slug must be unique. */
    CategoryResponse create(CategoryRequest request);

    /** Admin: updates a category. A slug that streams already use cannot be changed. */
    CategoryResponse update(String id, CategoryRequest request);

    /** Admin: deletes a category that no stream uses. */
    void delete(String id);
}
