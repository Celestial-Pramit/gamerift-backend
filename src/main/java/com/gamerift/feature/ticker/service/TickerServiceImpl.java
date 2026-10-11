package com.gamerift.feature.ticker.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.gamerift.feature.channel.model.Channel;
import com.gamerift.feature.channel.repository.ChannelRepository;
import com.gamerift.feature.event.model.GameEvent;
import com.gamerift.feature.event.repository.GameEventRepository;
import com.gamerift.feature.ticker.dto.TickerItemResponse;
import com.gamerift.feature.ticker.dto.TickerMessageRequest;
import com.gamerift.feature.ticker.dto.TickerMessageResponse;
import com.gamerift.feature.ticker.model.TickerMessage;
import com.gamerift.feature.ticker.repository.TickerMessageRepository;
import com.gamerift.shared.exception.NotFoundException;

import org.springframework.stereotype.Service;

/**
 * Business rules for the ticker (layer: service).
 * Called by: TickerController (feed) and AdminTickerController (messages).
 * Calls: ChannelRepository, GameEventRepository, TickerMessageRepository.
 * The feed is BUILT from live data every time (nothing about live channels or events is copied into the ticker).
 */
@Service
public class TickerServiceImpl implements ITickerService {

    // events lasting this long after the start are "live" (same 3-hour rule as the Events page)
    static final long EVENT_HOURS = 3;
    // upcoming events are shown in the ticker only when they start within this many hours
    static final long LOOK_AHEAD_HOURS = 24;
    static final int MAX_ITEMS = 20;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Asia/Dhaka"));

    private final ChannelRepository channelRepository;
    private final GameEventRepository eventRepository;
    private final TickerMessageRepository tickerRepository;

    public TickerServiceImpl(ChannelRepository channelRepository, GameEventRepository eventRepository,
                             TickerMessageRepository tickerRepository) {
        this.channelRepository = channelRepository;
        this.eventRepository = eventRepository;
        this.tickerRepository = tickerRepository;
    }

    @Override
    public List<TickerItemResponse> getFeed() {
        Instant now = Instant.now();
        List<TickerItemResponse> feed = new ArrayList<>();

        // 1. live channels
        for (Channel channel : channelRepository.findAll()) {
            if (channel.isLive()) {
                String note = (channel.getNote() == null || channel.getNote().isBlank()) ? "" : " - " + channel.getNote();
                feed.add(new TickerItemResponse("live", channel.getName() + " is LIVE" + note));
            }
        }

        // 2. events that are live now or start within the next 24 hours
        for (GameEvent event : eventRepository.findAll()) {
            if (now.isBefore(event.getStartsAt())) {
                long hours = Duration.between(now, event.getStartsAt()).toHours();
                if (hours < LOOK_AHEAD_HOURS) {
                    feed.add(new TickerItemResponse("event", event.getTitle() + " starts at " + TIME.format(event.getStartsAt()) + " BDT"));
                }
            } else if (now.isBefore(event.getStartsAt().plus(EVENT_HOURS, ChronoUnit.HOURS))) {
                feed.add(new TickerItemResponse("event", event.getTitle() + " is live now"));
            }
        }

        // 3. active messages written by the admin
        for (TickerMessage message : tickerRepository.findAll()) {
            if (message.isActive()) {
                feed.add(new TickerItemResponse("message", message.getText()));
            }
        }

        // 4. keep the bar short
        if (feed.size() > MAX_ITEMS) {
            return new ArrayList<>(feed.subList(0, MAX_ITEMS));
        }
        return feed;
    }

    @Override
    public List<TickerMessageResponse> getAllMessages() {
        List<TickerMessageResponse> result = new ArrayList<>();
        for (TickerMessage message : tickerRepository.findAll()) {
            result.add(toResponse(message));
        }
        return result;
    }

    @Override
    public TickerMessageResponse create(TickerMessageRequest request) {
        TickerMessage message = new TickerMessage();
        message.setText(request.getText().trim());
        message.setActive(request.isActive());
        return toResponse(tickerRepository.save(message));
    }

    @Override
    public TickerMessageResponse update(String id, TickerMessageRequest request) {
        // 1. the message must exist
        TickerMessage message = tickerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticker message not found"));
        // 2. overwrite and save
        message.setText(request.getText().trim());
        message.setActive(request.isActive());
        return toResponse(tickerRepository.save(message));
    }

    @Override
    public void delete(String id) {
        if (!tickerRepository.existsById(id)) {
            throw new NotFoundException("Ticker message not found");
        }
        tickerRepository.deleteById(id);
    }

    private TickerMessageResponse toResponse(TickerMessage message) {
        TickerMessageResponse r = new TickerMessageResponse();
        r.setId(message.getId());
        r.setText(message.getText());
        r.setActive(message.isActive());
        return r;
    }
}
