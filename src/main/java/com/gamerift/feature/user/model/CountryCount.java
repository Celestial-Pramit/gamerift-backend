package com.gamerift.feature.user.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Result row of the "users per country" aggregation. It is NOT stored in the database. */
@Getter
@Setter
@NoArgsConstructor
public class CountryCount {

    private String country;
    private long users;
}
