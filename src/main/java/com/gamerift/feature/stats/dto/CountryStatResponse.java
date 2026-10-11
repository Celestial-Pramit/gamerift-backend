package com.gamerift.feature.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** One row of "Users by country": a short code (BD), the name and how many users. */
@Getter
@AllArgsConstructor
public class CountryStatResponse {

    private String code;
    private String country;
    private long users;
}
