package com.gamerift.feature.category.repository;

import java.util.Optional;

import com.gamerift.feature.category.model.Category;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CategoryRepository extends MongoRepository<Category, String> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
