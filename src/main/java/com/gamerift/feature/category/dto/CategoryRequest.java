package com.gamerift.feature.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of the admin create / update category requests. @Valid rejects bad input with a 400 and field messages. */
@Getter
@Setter
@NoArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 40, message = "Name can have at most 40 characters")
    private String name;

    // Used in URLs, so only letters, digits and single hyphens. It is stored in lower case.
    @NotBlank(message = "Slug is required")
    @Size(max = 30, message = "Slug can have at most 30 characters")
    @Pattern(regexp = "^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$", message = "Slug can only contain letters, numbers and single hyphens")
    private String slug;

    @Size(max = 10, message = "Icon can have at most 10 characters")
    private String icon;
}
