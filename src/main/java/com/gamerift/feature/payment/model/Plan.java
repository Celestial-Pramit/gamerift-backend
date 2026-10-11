package com.gamerift.feature.payment.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** A Pro pass that users can buy (collection "plans"). Prices are placeholders in BDT. */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "plans")
public class Plan {

    @Id
    private String id;

    private String name;
    private int days;
    private int priceBdt;
    private String tagline;
    private List<String> perks = new ArrayList<>();
    private boolean bestValue;
    private boolean active = true;
}
