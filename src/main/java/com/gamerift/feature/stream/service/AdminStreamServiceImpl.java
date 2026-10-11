package com.gamerift.feature.stream.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gamerift.feature.category.repository.CategoryRepository;
import com.gamerift.feature.channel.model.Channel;
import com.gamerift.feature.channel.model.Follow;
import com.gamerift.feature.channel.repository.ChannelRepository;
import com.gamerift.feature.channel.repository.FollowRepository;
import com.gamerift.feature.notification.service.INotificationService;
import com.gamerift.feature.stream.dto.StreamRequest;
import com.gamerift.feature.stream.dto.StreamResponse;
import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.model.StreamPhase;
import com.gamerift.feature.stream.model.VideoSource;
import com.gamerift.feature.stream.repository.LiveStreamRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.NotFoundException;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Business rules for managing streams (layer: service, ADMIN only).
 *  - the category and the channel must exist,
 *  - a YouTube link must really be a YouTube link; LIVE_HLS is "coming soon",
 *  - a SOON stream needs a start time in the future; a LIVE stream gets its start time now,
 *  - a draft cannot be featured and cannot go live,
 *  - only ONE stream is featured (the hero): featuring a new one removes the flag from the old one.
 */
@Service
public class AdminStreamServiceImpl implements IAdminStreamService {

    // groups: 4 = the 11-character video id
    private static final Pattern YOUTUBE =
            Pattern.compile("^(https?://)?(www\\.)?(youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([\\w-]{11})([&?].*)?$");

    private final LiveStreamRepository liveStreamRepository;
    private final CategoryRepository categoryRepository;
    private final ChannelRepository channelRepository;
    private final INotificationService notificationService;
    private final FollowRepository followRepository;

    public AdminStreamServiceImpl(LiveStreamRepository liveStreamRepository,
                                  CategoryRepository categoryRepository,
                                  ChannelRepository channelRepository,
                                  INotificationService notificationService,
                                  FollowRepository followRepository) {
        this.liveStreamRepository = liveStreamRepository;
        this.categoryRepository = categoryRepository;
        this.channelRepository = channelRepository;
        this.notificationService = notificationService;
        this.followRepository = followRepository;
    }

    @Override
    public List<StreamResponse> getAll() {
        List<LiveStream> streams = liveStreamRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        // one query for all channels: channelId -> is Pro?
        Set<String> channelIds = new HashSet<>();
        for (LiveStream stream : streams) {
            if (stream.getChannelId() != null) {
                channelIds.add(stream.getChannelId());
            }
        }
        Map<String, Boolean> proByChannel = new HashMap<>();
        for (Channel channel : channelRepository.findAllById(channelIds)) {
            proByChannel.put(channel.getId(), channel.isPro());
        }
        Instant now = Instant.now();
        List<StreamResponse> result = new ArrayList<>();
        for (LiveStream stream : streams) {
            result.add(StreamMapper.toResponse(stream, proByChannel.getOrDefault(stream.getChannelId(), false), now));
        }
        return result;
    }

    @Override
    public StreamResponse create(StreamRequest request) {
        LiveStream stream = new LiveStream();
        apply(stream, request);
        return toResponse(liveStreamRepository.save(stream));
    }

    @Override
    public StreamResponse update(String id, StreamRequest request) {
        LiveStream stream = findById(id);
        apply(stream, request);
        return toResponse(liveStreamRepository.save(stream));
    }

    @Override
    public StreamResponse setLive(String id, boolean live) {
        LiveStream stream = findById(id);
        boolean wentLive = false;
        if (live) {
            if (!stream.isPublished()) {
                throw new BadRequestException("A draft cannot go live. Publish it first");
            }
            if (stream.getPhase() != StreamPhase.LIVE) {
                stream.setPhase(StreamPhase.LIVE);
                stream.setStartedAt(Instant.now());
                stream.setScheduledAt(null);
                wentLive = true;
            }
        } else {
            stream.setPhase(StreamPhase.ENDED);
        }
        StreamResponse response = toResponse(liveStreamRepository.save(stream));
        if (wentLive) {
            try {
                notificationService.notifyChannelLive(
                        stream.getChannelId(),
                        stream.getChannelName(),
                        stream.getTitle(),
                        "/app/stream/" + stream.getId());
            } catch (Exception e) {
                // Notification failure must never fail the admin action
            }
        }
        return response;
    }

    @Override
    public StreamResponse setPublished(String id, boolean published) {
        LiveStream stream = findById(id);
        stream.setPublished(published);
        if (!published) {
            // a draft can never be the hero stream
            stream.setFeatured(false);
        }
        return toResponse(liveStreamRepository.save(stream));
    }

    @Override
    public void delete(String id) {
        liveStreamRepository.delete(findById(id));
    }

    // ---------------------------------------------------------------- helpers

    // Copies the form into the stream after checking the rules. Used by create AND update.
    private void apply(LiveStream stream, StreamRequest request) {
        // 1. the category and the channel must exist
        String slug = request.getCategorySlug().trim().toLowerCase();
        if (!categoryRepository.existsBySlug(slug)) {
            throw new BadRequestException("Unknown category: " + slug);
        }
        Channel channel = channelRepository.findById(request.getChannelId())
                .orElseThrow(() -> new BadRequestException("Unknown channel"));

        // 2. a draft cannot be featured
        if (request.isFeatured() && !request.isPublished()) {
            throw new BadRequestException("A draft cannot be featured");
        }

        // 3. the video
        applyVideo(stream, request.getVideoSource(), request.getVideoUrl());

        // 4. the phase and its times
        applyPhase(stream, request);

        // 5. only one featured stream: remove the flag from the others
        if (request.isFeatured()) {
            for (LiveStream other : liveStreamRepository.findByFeaturedTrue()) {
                if (other.getId() != null && !other.getId().equals(stream.getId())) {
                    other.setFeatured(false);
                    liveStreamRepository.save(other);
                }
            }
        }

        // 6. the simple fields
        stream.setTitle(request.getTitle().trim());
        stream.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        stream.setCategorySlug(slug);
        stream.setChannelId(channel.getId());
        stream.setChannelName(channel.getName());
        stream.setBaseViewers(request.getBaseViewers());
        stream.setPublished(request.isPublished());
        stream.setFeatured(request.isFeatured());
        stream.setTags(cleanTags(request.getTags()));
    }

    private void applyVideo(LiveStream stream, VideoSource source, String urlInput) {
        String url = urlInput == null ? "" : urlInput.trim();
        String thumbnail = "";
        if (source == VideoSource.LIVE_HLS) {
            throw new BadRequestException("Real live streaming is coming soon");
        }
        if (source == VideoSource.YOUTUBE && !url.isEmpty()) {
            Matcher matcher = YOUTUBE.matcher(url);
            if (!matcher.matches()) {
                throw new BadRequestException("That is not a valid YouTube link");
            }
            thumbnail = "https://img.youtube.com/vi/" + matcher.group(4) + "/hqdefault.jpg";
        }
        if (source == VideoSource.CLOUDINARY && !url.isEmpty() && !url.startsWith("https://")) {
            throw new BadRequestException("The video link must start with https://");
        }
        stream.setVideoSource(source);
        stream.setVideoUrl(url);
        stream.setThumbnailUrl(thumbnail);
    }

    private void applyPhase(LiveStream stream, StreamRequest request) {
        StreamPhase phase = request.getPhase();
        if (phase == StreamPhase.SOON) {
            if (request.getScheduledAt() == null || !request.getScheduledAt().isAfter(Instant.now())) {
                throw new BadRequestException("A stream that starts soon needs a start time in the future");
            }
            stream.setScheduledAt(request.getScheduledAt());
            stream.setStartedAt(null);
        } else if (phase == StreamPhase.LIVE) {
            // it starts now, unless it was already live
            if (stream.getPhase() != StreamPhase.LIVE || stream.getStartedAt() == null) {
                stream.setStartedAt(Instant.now());
            }
            stream.setScheduledAt(null);
        }
        stream.setPhase(phase);
    }

    // lower case, trimmed, no empty values, no duplicates
    private List<String> cleanTags(List<String> tags) {
        List<String> result = new ArrayList<>();
        if (tags != null) {
            for (String tag : tags) {
                String clean = tag == null ? "" : tag.trim().toLowerCase();
                if (!clean.isEmpty() && !result.contains(clean)) {
                    result.add(clean);
                }
            }
        }
        return result;
    }

    private LiveStream findById(String id) {
        return liveStreamRepository.findById(id).orElseThrow(() -> new NotFoundException("Stream not found"));
    }

    private StreamResponse toResponse(LiveStream stream) {
        boolean pro = false;
        if (stream.getChannelId() != null) {
            Optional<Channel> channel = channelRepository.findById(stream.getChannelId());
            pro = channel.isPresent() && channel.get().isPro();
        }
        return StreamMapper.toResponse(stream, pro, Instant.now());
    }
}
