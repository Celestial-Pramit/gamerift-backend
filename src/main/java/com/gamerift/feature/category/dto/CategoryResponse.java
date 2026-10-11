package com.gamerift.feature.category.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DTO (Data Transfer Object) = exactly the data we want to send to the frontend.
 * Controllers return DTOs, never database models, so internal fields can never leak by accident.
 */
@Getter
@AllArgsConstructor
public class CategoryResponse {

    private String id;
    private String name;
    private String slug;
    private String icon;
}
