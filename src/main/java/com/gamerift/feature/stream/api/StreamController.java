package com.gamerift.feature.stream.api;

import java.util.List;

import com.gamerift.feature.stream.dto.StreamResponse;
import com.gamerift.feature.stream.service.IStreamService;
import com.gamerift.shared.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller (layer: HTTP) for reading streams. All URLs need a login.
 *
 * Request flow of the Discover page: Browser -> StreamController -> IStreamService (StreamServiceImpl)
 *   -> StreamSearchRepository (MongoTemplate query) + ChannelRepository -> MongoDB -> PageResponse JSON.
 *
 * @RequestParam reads ?name=value from the URL. required = false / defaultValue make a filter optional.
 * Fixed words ("featured", "trending") are mapped before "/{id}" so they are not mistaken for an id.
 */
@RestController
@RequestMapping("/api/streams")
@Tag(name = "Streams", description = "Discover, detail, featured, trending, related")
public class StreamController {

    private final IStreamService streamService;

    public StreamController(IStreamService streamService) {
        this.streamService = streamService;
    }

    @GetMapping
    @Operation(summary = "Search, filter, sort and page the streams (Discover page)")
    public PageResponse<StreamResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(defaultValue = "0") int minViewers,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "viewers") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return streamService.search(q, category, tags, minViewers, status, sort, page, size);
    }

    @GetMapping("/featured")
    @Operation(summary = "The featured (hero) stream")
    public StreamResponse getFeatured() {
        return streamService.getFeatured();
    }

    @GetMapping("/trending")
    @Operation(summary = "Top live streams by trend score")
    public List<StreamResponse> getTrending() {
        return streamService.getTrending();
    }

    @GetMapping("/{id}")
    @Operation(summary = "One stream (404 if missing or a draft)")
    public StreamResponse getById(@PathVariable String id) {
        return streamService.getById(id);
    }

    @GetMapping("/{id}/related")
    @Operation(summary = "Up to 6 other streams of the same category")
    public List<StreamResponse> getRelated(@PathVariable String id) {
        return streamService.getRelated(id);
    }
}
