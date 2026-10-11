package com.gamerift.feature.category.service;

import java.util.ArrayList;
import java.util.List;

import com.gamerift.feature.category.dto.CategoryRequest;
import com.gamerift.feature.category.dto.CategoryResponse;
import com.gamerift.feature.category.model.Category;
import com.gamerift.feature.category.repository.CategoryRepository;
import com.gamerift.feature.stream.repository.LiveStreamRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.ConflictException;
import com.gamerift.shared.exception.NotFoundException;

import org.springframework.stereotype.Service;

/**
 * Service IMPLEMENTATION (layer: business logic) of the category feature.
 * All rules live here:
 *  - the slug must be unique (and is stored in lower case),
 *  - a slug that streams use cannot be changed (the streams would lose their category),
 *  - a category that streams use cannot be deleted.
 *
 * @Service registers the class as a Spring bean. The repositories are passed through the
 * constructor (constructor injection): Spring creates them and gives them to us.
 */
@Service
public class CategoryServiceImpl implements ICategoryService {

    private final CategoryRepository categoryRepository;
    private final LiveStreamRepository liveStreamRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, LiveStreamRepository liveStreamRepository) {
        this.categoryRepository = categoryRepository;
        this.liveStreamRepository = liveStreamRepository;
    }

    @Override
    public List<CategoryResponse> getAll() {
        // 1. load every category from MongoDB
        List<Category> categories = categoryRepository.findAll();
        // 2. convert each model to a DTO
        List<CategoryResponse> result = new ArrayList<>();
        for (Category category : categories) {
            result.add(CategoryMapper.toResponse(category));
        }
        return result;
    }

    @Override
    public CategoryResponse getBySlug(String slug) {
        // 1. look it up; 2. if it does not exist, throw our NotFoundException (-> HTTP 404 JSON)
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Category not found: " + slug));
        return CategoryMapper.toResponse(category);
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {
        // 1. clean the slug, 2. it must be unique, 3. save
        String slug = request.getSlug().trim().toLowerCase();
        if (categoryRepository.existsBySlug(slug)) {
            throw new ConflictException("Slug already used: " + slug);
        }
        Category category = new Category();
        category.setName(request.getName().trim());
        category.setSlug(slug);
        category.setIcon(request.getIcon());
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse update(String id, CategoryRequest request) {
        Category category = findById(id);
        String newSlug = request.getSlug().trim().toLowerCase();

        // the slug is only checked when it really changes
        if (!newSlug.equals(category.getSlug())) {
            if (categoryRepository.existsBySlug(newSlug)) {
                throw new ConflictException("Slug already used: " + newSlug);
            }
            if (liveStreamRepository.existsByCategorySlug(category.getSlug())) {
                throw new BadRequestException("The slug cannot be changed while streams use this category");
            }
            category.setSlug(newSlug);
        }
        category.setName(request.getName().trim());
        category.setIcon(request.getIcon());
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public void delete(String id) {
        Category category = findById(id);
        if (liveStreamRepository.existsByCategorySlug(category.getSlug())) {
            throw new ConflictException("Streams use this category, so it cannot be deleted");
        }
        categoryRepository.delete(category);
    }

    private Category findById(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }
}
