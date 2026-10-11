package com.gamerift.feature.stats.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The "Streams by category" donut of the admin overview: one row per category. */
@Getter
@Setter
@NoArgsConstructor
public class CategoryCountResponse {

    private String category;
    private String slug;
    private long count;

    public CategoryCountResponse(String category, String slug, long count) {
        this.category = category;
        this.slug = slug;
        this.count = count;
    }
}