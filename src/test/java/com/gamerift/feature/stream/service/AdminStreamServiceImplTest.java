package com.gamerift.feature.stream.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.gamerift.feature.category.repository.CategoryRepository;
import com.gamerift.feature.channel.model.Channel;
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the admin stream rules. Repositories are fakes. */
@ExtendWith(MockitoExtension.class)
class AdminStreamServiceImplTest {

    @Mock
    private LiveStreamRepository liveStreamRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private INotificationService notificationService;
    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private AdminStreamServiceImpl service;

    private StreamRequest request() {
        StreamRequest r = new StreamRequest();
        r.setTitle("  Grand Finals  ");
        r.setCategorySlug("Esports");
        r.setChannelId("c1");
        r.setVideoSource(VideoSource.YOUTUBE);
        r.setVideoUrl("https://youtu.be/abcdefghijk");
        r.setPhase(StreamPhase.ENDED);
        r.setBaseViewers(1000);
        r.setPublished(true);
        r.setFeatured(false);
        r.setTags(List.of(" Valorant ", "valorant", "", "BANGLA"));
        return r;
    }

    private Channel channel() {
        Channel c = new Channel();
        c.setId("c1");
        c.setName("NightRaid");
        return c;
    }

    private void categoryAndChannelExist() {
        when(categoryRepository.existsBySlug("esports")).thenReturn(true);
        when(channelRepository.findById("c1")).thenReturn(Optional.of(channel()));
    }

    private LiveStream stored(String id, boolean published, StreamPhase phase) {
        LiveStream s = new LiveStream();
        s.setId(id);
        s.setPublished(published);
        s.setPhase(phase);
        return s;
    }

    @Test
    void create_validRequest_copiesChannelNameThumbnailAndCleansTags() {
        categoryAndChannelExist();
        when(liveStreamRepository.save(any(LiveStream.class))).thenAnswer(call -> call.getArgument(0));

        StreamResponse response = service.create(request());

        assertEquals("Grand Finals", response.getTitle());
        assertEquals("esports", response.getCategorySlug());
        assertEquals("NightRaid", response.getChannelName());
        assertEquals(List.of("valorant", "bangla"), response.getTags());
        assertEquals("https://img.youtube.com/vi/abcdefghijk/hqdefault.jpg", response.getThumbnailUrl());
    }

    @Test
    void create_unknownCategory_throwsBadRequest() {
        when(categoryRepository.existsBySlug("esports")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> service.create(request()));
        verify(liveStreamRepository, never()).save(any(LiveStream.class));
    }

    @Test
    void create_unknownChannel_throwsBadRequest() {
        when(categoryRepository.existsBySlug("esports")).thenReturn(true);
        when(channelRepository.findById("c1")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> service.create(request()));
    }

    @Test
    void create_notAYoutubeLink_throwsBadRequest() {
        categoryAndChannelExist();
        StreamRequest r = request();
        r.setVideoUrl("https://example.com/video");

        assertThrows(BadRequestException.class, () -> service.create(r));
    }

    @Test
    void create_liveHlsSource_isNotAvailableYet() {
        categoryAndChannelExist();
        StreamRequest r = request();
        r.setVideoSource(VideoSource.LIVE_HLS);

        assertThrows(BadRequestException.class, () -> service.create(r));
    }

    @Test
    void create_soonWithoutAFutureStartTime_throwsBadRequest() {
        categoryAndChannelExist();
        StreamRequest r = request();
        r.setPhase(StreamPhase.SOON);
        r.setScheduledAt(Instant.now().minusSeconds(60));

        assertThrows(BadRequestException.class, () -> service.create(r));
    }

    @Test
    void create_livePhase_startsNow() {
        categoryAndChannelExist();
        when(liveStreamRepository.save(any(LiveStream.class))).thenAnswer(call -> call.getArgument(0));
        StreamRequest r = request();
        r.setPhase(StreamPhase.LIVE);

        StreamResponse response = service.create(r);

        assertEquals("live", response.getStatus());
        assertNotNull(response.getStartedAt());
    }

    @Test
    void create_featuredDraft_throwsBadRequest() {
        categoryAndChannelExist();
        StreamRequest r = request();
        r.setPublished(false);
        r.setFeatured(true);

        assertThrows(BadRequestException.class, () -> service.create(r));
    }

    @Test
    void create_featured_removesTheFlagFromTheOldHero() {
        categoryAndChannelExist();
        LiveStream oldHero = stored("old", true, StreamPhase.LIVE);
        oldHero.setFeatured(true);
        when(liveStreamRepository.findByFeaturedTrue()).thenReturn(List.of(oldHero));
        when(liveStreamRepository.save(any(LiveStream.class))).thenAnswer(call -> call.getArgument(0));
        StreamRequest r = request();
        r.setFeatured(true);

        service.create(r);

        assertFalse(oldHero.isFeatured());
        verify(liveStreamRepository).save(oldHero);
    }

    @Test
    void setLive_onADraft_throwsBadRequest() {
        when(liveStreamRepository.findById("s1")).thenReturn(Optional.of(stored("s1", false, StreamPhase.SOON)));

        assertThrows(BadRequestException.class, () -> service.setLive("s1", true));
    }

    @Test
    void setLive_true_startsTheStreamNow() {
        LiveStream s = stored("s1", true, StreamPhase.SOON);
        when(liveStreamRepository.findById("s1")).thenReturn(Optional.of(s));
        when(liveStreamRepository.save(any(LiveStream.class))).thenAnswer(call -> call.getArgument(0));

        service.setLive("s1", true);

        assertEquals(StreamPhase.LIVE, s.getPhase());
        assertNotNull(s.getStartedAt());
    }

    @Test
    void setLive_false_endsTheStream() {
        LiveStream s = stored("s1", true, StreamPhase.LIVE);
        when(liveStreamRepository.findById("s1")).thenReturn(Optional.of(s));
        when(liveStreamRepository.save(any(LiveStream.class))).thenAnswer(call -> call.getArgument(0));

        service.setLive("s1", false);

        assertEquals(StreamPhase.ENDED, s.getPhase());
    }

    @Test
    void setPublished_false_alsoRemovesTheFeaturedFlag() {
        LiveStream s = stored("s1", true, StreamPhase.LIVE);
        s.setFeatured(true);
        when(liveStreamRepository.findById("s1")).thenReturn(Optional.of(s));
        when(liveStreamRepository.save(any(LiveStream.class))).thenAnswer(call -> call.getArgument(0));

        StreamResponse response = service.setPublished("s1", false);

        assertFalse(response.isPublished());
        assertFalse(response.isFeatured());
    }

    @Test
    void delete_existingStream_isRemoved() {
        LiveStream s = stored("s1", true, StreamPhase.ENDED);
        when(liveStreamRepository.findById("s1")).thenReturn(Optional.of(s));

        service.delete("s1");

        verify(liveStreamRepository).delete(s);
    }

    @Test
    void delete_unknownStream_throwsNotFound() {
        when(liveStreamRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete("zzz"));
        assertTrue(true);
    }
}
