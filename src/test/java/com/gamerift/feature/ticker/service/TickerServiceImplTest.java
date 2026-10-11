package com.gamerift.feature.ticker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the ticker feed rules and the admin message CRUD. Repositories are fakes. */
@ExtendWith(MockitoExtension.class)
class TickerServiceImplTest {

    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private GameEventRepository eventRepository;
    @Mock
    private TickerMessageRepository tickerRepository;

    @InjectMocks
    private TickerServiceImpl tickerService;

    private Channel channel(String name, boolean live) {
        Channel c = new Channel();
        c.setName(name);
        c.setLive(live);
        return c;
    }

    private GameEvent event(String title, long hoursFromNow) {
        GameEvent e = new GameEvent();
        e.setTitle(title);
        e.setStartsAt(Instant.now().plus(hoursFromNow, ChronoUnit.HOURS));
        return e;
    }

    private TickerMessage message(String id, String text, boolean active) {
        TickerMessage m = new TickerMessage();
        m.setId(id);
        m.setText(text);
        m.setActive(active);
        return m;
    }

    private TickerMessageRequest request(String text, boolean active) {
        TickerMessageRequest r = new TickerMessageRequest();
        r.setText(text);
        r.setActive(active);
        return r;
    }

    @Test
    void feed_putsLiveChannelsThenEventsThenMessages() {
        when(channelRepository.findAll()).thenReturn(List.of(channel("NightRaid", true), channel("Quiet", false)));
        when(eventRepository.findAll()).thenReturn(List.of(event("Cup", 2)));
        when(tickerRepository.findAll()).thenReturn(List.of(message("m1", "Welcome", true)));

        List<TickerItemResponse> feed = tickerService.getFeed();

        assertEquals(3, feed.size());
        assertEquals("live", feed.get(0).getType());
        assertTrue(feed.get(0).getText().contains("NightRaid"));
        assertEquals("event", feed.get(1).getType());
        assertEquals("message", feed.get(2).getType());
    }

    @Test
    void feed_skipsOfflineChannelsAndInactiveMessages() {
        when(channelRepository.findAll()).thenReturn(List.of(channel("Quiet", false)));
        when(eventRepository.findAll()).thenReturn(List.of());
        when(tickerRepository.findAll()).thenReturn(List.of(message("m1", "Hidden", false)));

        assertTrue(tickerService.getFeed().isEmpty());
    }

    @Test
    void feed_eventsFarInTheFutureOrAlreadyOverAreLeftOut() {
        when(channelRepository.findAll()).thenReturn(List.of());
        when(eventRepository.findAll()).thenReturn(List.of(event("Far", 100), event("Old", -10), event("LiveNow", -1)));
        when(tickerRepository.findAll()).thenReturn(List.of());

        List<TickerItemResponse> feed = tickerService.getFeed();

        assertEquals(1, feed.size());
        assertTrue(feed.get(0).getText().contains("LiveNow"));
        assertTrue(feed.get(0).getText().contains("live now"));
    }

    @Test
    void feed_isCutAtTwentyItems() {
        when(channelRepository.findAll()).thenReturn(List.of());
        when(eventRepository.findAll()).thenReturn(List.of());
        List<TickerMessage> many = new java.util.ArrayList<>();
        for (int i = 0; i < 30; i++) {
            many.add(message("m" + i, "Msg " + i, true));
        }
        when(tickerRepository.findAll()).thenReturn(many);

        assertEquals(20, tickerService.getFeed().size());
    }

    @Test
    void create_trimsTheTextAndSaves() {
        when(tickerRepository.save(any(TickerMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        TickerMessageResponse response = tickerService.create(request("  Hello  ", true));

        assertEquals("Hello", response.getText());
        assertTrue(response.isActive());
    }

    @Test
    void update_changesTextAndActive() {
        when(tickerRepository.findById("m1")).thenReturn(Optional.of(message("m1", "Old", true)));
        when(tickerRepository.save(any(TickerMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        TickerMessageResponse response = tickerService.update("m1", request("New", false));

        assertEquals("New", response.getText());
        assertEquals(false, response.isActive());
    }

    @Test
    void update_unknownId_throwsNotFound() {
        when(tickerRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> tickerService.update("zzz", request("x", true)));
    }

    @Test
    void delete_existing_deletes() {
        when(tickerRepository.existsById("m1")).thenReturn(true);

        tickerService.delete("m1");

        verify(tickerRepository).deleteById("m1");
    }

    @Test
    void delete_unknownId_throwsNotFound() {
        when(tickerRepository.existsById("zzz")).thenReturn(false);

        assertThrows(NotFoundException.class, () -> tickerService.delete("zzz"));
        verify(tickerRepository, never()).deleteById(any());
    }
}
