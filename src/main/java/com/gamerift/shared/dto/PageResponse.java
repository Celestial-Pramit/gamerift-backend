package com.gamerift.shared.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * One page of results: the items plus the numbers the frontend needs to draw "Load more" or page buttons.
 * T is a type placeholder (generic): PageResponse<StreamResponse> holds streams, PageResponse<X> holds X.
 */
@Getter
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    // page number, starting at 0
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
