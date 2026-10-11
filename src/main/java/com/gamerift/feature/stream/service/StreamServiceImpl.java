package com.gamerift.feature.stream.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gamerift.feature.channel.model.Channel;
import com.gamerift.feature.channel.repository.ChannelRepository;
import com.gamerift.feature.stream.dto.StreamResponse;
import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.model.StreamPhase;
import com.gamerift.feature.stream.repository.LiveStreamRepository;
import com.gamerift.feature.stream.repository.StreamSearchRepository;
import com.gamerift.shared.dto.PageResponse;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.NotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Business rules for reading streams (layer: service).
 *  - page size is limited to 50 so nobody can ask for the whole database,
 *  - unknown sort / status values are rejected with a clear 400,
 *  - drafts (published = false) are never shown to normal users,
 *  - the "pro" flag of each stream comes from its channel (one batch query per page, not one per stream).
 */
@Service
public class StreamServiceImpl implements IStreamService {

    private static final int MAX_PAGE_SIZE = 50;

    private final LiveStreamRepository liveStreamRepository;
    private final StreamSearchRepository streamSearchRepository;
    private final ChannelRepository channelRepository;

    public StreamServiceImpl(LiveStreamRepository liveStreamRepository,
                             StreamSearchRepository streamSearchRepository,
                             ChannelRepository channelRepository) {
        this.liveStreamRepository = liveStreamRepository;
        this.streamSearchRepository = streamSearchRepository;
        this.channelRepository = channelRepository;
    }

    @Override
    public PageResponse<StreamResponse> search(String text, String category, List<String> tags, int minViewers,
                                               String status, String sort, int page, int size) {
        // 1. clean the paging numbers, 2. translate "sort" and "status" words, 3. ask the repository
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, parseSort(sort));
        StreamPhase phase = parsePhase(status);

        Page<LiveStream> result = streamSearchRepository.search(text, category, tags, minViewers, phase, pageable);

        // 4. convert to DTOs
        Map<String, Boolean> proByChannel = loadProFlags(result.getContent());
        Instant now = Instant.now();
        List<StreamResponse> items = new ArrayList<>();
        for (LiveStream stream : result.getContent()) {
            items.add(StreamMapper.toResponse(stream, proByChannel.getOrDefault(stream.getChannelId(), false), now));
        }
        return new PageResponse<>(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public StreamResponse getById(String id) {
        return toResponse(findPublished(id));
    }

    @Override
    public StreamResponse getFeatured() {
        LiveStream stream = liveStreamRepository.findFirstByPublishedTrueAndFeaturedTrueOrderByBaseViewersDesc()
                .orElseThrow(() -> new NotFoundException("No featured stream"));
        return toResponse(stream);
    }

    @Override
    public List<StreamResponse> getTrending() {
        return toResponses(liveStreamRepository.findTop8ByPublishedTrueAndPhaseOrderByTrendScoreDesc(StreamPhase.LIVE));
    }

    @Override
    public List<StreamResponse> getRelated(String id) {
        LiveStream stream = findPublished(id);
        List<LiveStream> others = new ArrayList<>();
        for (LiveStream candidate : liveStreamRepository.findTop7ByCategorySlugAndPublishedTrueOrderByBaseViewersDesc(stream.getCategorySlug())) {
            // not the stream itself, and at most 6
            if (!candidate.getId().equals(stream.getId()) && others.size() < 6) {
                others.add(candidate);
            }
        }
        return toResponses(others);
    }

    // ---------------------------------------------------------------- helpers

    private LiveStream findPublished(String id) {
        LiveStream stream = liveStreamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Stream not found"));
        // a draft looks like "does not exist" to normal users
        if (!stream.isPublished()) {
            throw new NotFoundException("Stream not found");
        }
        return stream;
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.equals("viewers")) {
            return Sort.by(Sort.Direction.DESC, "baseViewers");
        }
        if (sort.equals("viewersAsc")) {
            return Sort.by(Sort.Direction.ASC, "baseViewers");
        }
        if (sort.equals("newest")) {
            return Sort.by(Sort.Direction.DESC, "startedAt");
        }
        if (sort.equals("trending")) {
            return Sort.by(Sort.Direction.DESC, "trendScore");
        }
        throw new BadRequestException("sort must be viewers, viewersAsc, newest or trending");
    }

    private StreamPhase parsePhase(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("all")) {
            return null;
        }
        if (status.equalsIgnoreCase("live")) {
            return StreamPhase.LIVE;
        }
        if (status.equalsIgnoreCase("soon")) {
            return StreamPhase.SOON;
        }
        if (status.equalsIgnoreCase("ended")) {
            return StreamPhase.ENDED;
        }
        throw new BadRequestException("status must be live, soon, ended or all");
    }

    private StreamResponse toResponse(LiveStream stream) {
        boolean pro = false;
        if (stream.getChannelId() != null) {
            pro = channelRepository.findById(stream.getChannelId()).map(Channel::isPro).orElse(false);
        }
        return StreamMapper.toResponse(stream, pro, Instant.now());
    }

    private List<StreamResponse> toResponses(List<LiveStream> streams) {
        Map<String, Boolean> proByChannel = loadProFlags(streams);
        Instant now = Instant.now();
        List<StreamResponse> result = new ArrayList<>();
        for (LiveStream stream : streams) {
            result.add(StreamMapper.toResponse(stream, proByChannel.getOrDefault(stream.getChannelId(), false), now));
        }
        return result;
    }

    // One query for all channels of a page: channelId -> is Pro?
    private Map<String, Boolean> loadProFlags(List<LiveStream> streams) {
        Set<String> channelIds = new HashSet<>();
        for (LiveStream stream : streams) {
            if (stream.getChannelId() != null) {
                channelIds.add(stream.getChannelId());
            }
        }
        Map<String, Boolean> flags = new HashMap<>();
        for (Channel channel : channelRepository.findAllById(channelIds)) {
            flags.put(channel.getId(), channel.isPro());
        }
        return flags;
    }
}
