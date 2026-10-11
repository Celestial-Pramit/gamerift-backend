package com.gamerift.feature.category.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** A game category such as Esports or FPS (collection "categories"). The slug is unique. */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "categories")
public class Category {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String slug;

    private String icon;
}
