package com.gamerift.feature.stream.service;

import java.util.List;

import com.gamerift.feature.stream.dto.StreamResponse;
import com.gamerift.shared.dto.PageResponse;

/** What the stream feature can do (read side). Admin create/update/delete comes in the next part. */
public interface IStreamService {

    /** Discover page: search + filters + sorting + paging. */
    PageResponse<StreamResponse> search(String text, String category, List<String> tags, int minViewers,
                                        String status, String sort, int page, int size);

    StreamResponse getById(String id);

    /** The hero stream of the dashboard. */
    StreamResponse getFeatured();

    /** The best live streams by trend score. */
    List<StreamResponse> getTrending();

    /** Up to 6 other streams of the same category. */
    List<StreamResponse> getRelated(String id);
}
