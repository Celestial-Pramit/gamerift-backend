package com.gamerift.feature.stream.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.gamerift.feature.channel.model.Follow;
import com.gamerift.feature.channel.repository.ChannelRepository;
import com.gamerift.feature.channel.repository.FollowRepository;
import com.gamerift.feature.notification.service.INotificationService;
import com.gamerift.feature.stream.model.LiveStream;
import com.gamerift.feature.stream.model.StreamPhase;
import com.gamerift.feature.stream.repository.LiveStreamRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * R3 (gap requirement): when a published stream really goes live, the followers of its
 * channel are told with a "live" alert. Repositories are fakes.
 */
@ExtendWith(MockitoExtension.class)
class AdminStreamLiveNotifyTest {

    @Mock
    private LiveStreamRepository liveStreamRepository;
    @Mock
    private com.gamerift.feature.category.repository.CategoryRepository categoryRepository;
    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private INotificationService notificationService;
    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private AdminStreamServiceImpl service;

    private LiveStream stream(String phasePublish) {
        LiveStream s = new LiveStream();
        s.setId("s1");
        s.setChannelId("c1");
        s.setTitle("Grand Finals");
        s.setChannelName("Grand Finals");
        s.setPublished(true);
        s.setPhase(StreamPhase.SOON);
        return s;
    }

    @Test
    void setLive_true_notifiesTheFollowersOfTheChannel() {
        LiveStream s = stream("soon");
        when(liveStreamRepository.findById("s1")).thenReturn(Optional.of(s));
        when(liveStreamRepository.save(any(LiveStream.class))).thenAnswer(inv -> inv.getArgument(0));

        service.setLive("s1", true);

        verify(notificationService).notifyChannelLive("c1", "Grand Finals", "Grand Finals", "/app/stream/s1");
    }

    @Test
    void setLive_whenAlreadyLive_doesNotNotifyTwice() {
        LiveStream s = stream("soon");
        s.setPhase(StreamPhase.LIVE);
        when(liveStreamRepository.findById("s1")).thenReturn(Optional.of(s));
        when(liveStreamRepository.save(any(LiveStream.class))).thenAnswer(inv -> inv.getArgument(0));

        service.setLive("s1", true);

        verify(notificationService, never()).notifyChannelLive(anyString(), anyString(), anyString(), anyString());
        verify(liveStreamRepository).save(s);
    }
}