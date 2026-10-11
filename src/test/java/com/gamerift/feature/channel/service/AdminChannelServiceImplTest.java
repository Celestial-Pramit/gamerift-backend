package com.gamerift.feature.channel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.gamerift.feature.channel.dto.AdminChannelResponse;
import com.gamerift.feature.channel.dto.ChannelRequest;
import com.gamerift.feature.channel.model.Channel;
import com.gamerift.feature.channel.repository.ChannelRepository;
import com.gamerift.feature.stream.repository.LiveStreamRepository;
import com.gamerift.shared.exception.ConflictException;
import com.gamerift.shared.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests of the admin channel rules (unique name, keep followers, delete guard). Repositories are fakes. */
@ExtendWith(MockitoExtension.class)
class AdminChannelServiceImplTest {

    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private LiveStreamRepository streamRepository;

    @InjectMocks
    private AdminChannelServiceImpl service;

    private Channel channel(String id, String name, long followers) {
        Channel c = new Channel();
        c.setId(id);
        c.setName(name);
        c.setFollowersCount(followers);
        return c;
    }

    private ChannelRequest request(String name) {
        ChannelRequest r = new ChannelRequest();
        r.setName(name);
        r.setVerified(true);
        r.setNote("note");
        return r;
    }

    @Test
    void getAll_isSortedByNameIgnoringCase() {
        when(channelRepository.findAll()).thenReturn(List.of(channel("1", "zeta", 0), channel("2", "Alpha", 0)));

        List<AdminChannelResponse> result = service.getAll();

        assertEquals("Alpha", result.get(0).getName());
        assertEquals("zeta", result.get(1).getName());
    }

    @Test
    void create_savesWithZeroFollowers() {
        when(channelRepository.findByName("NewOne")).thenReturn(Optional.empty());
        when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminChannelResponse response = service.create(request("  NewOne "));

        assertEquals("NewOne", response.getName());
        assertEquals(0, response.getFollowersCount());
        assertEquals(true, response.isVerified());
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(channelRepository.findByName("Taken")).thenReturn(Optional.of(channel("1", "Taken", 5)));

        assertThrows(ConflictException.class, () -> service.create(request("Taken")));
        verify(channelRepository, never()).save(any(Channel.class));
    }

    @Test
    void update_keepsFollowersAndChangesFields() {
        when(channelRepository.findById("1")).thenReturn(Optional.of(channel("1", "Old", 42)));
        when(channelRepository.findByName("Renamed")).thenReturn(Optional.empty());
        when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminChannelResponse response = service.update("1", request("Renamed"));

        assertEquals("Renamed", response.getName());
        assertEquals(42, response.getFollowersCount());
    }

    @Test
    void update_sameNameOnSameChannel_isAllowed() {
        Channel existing = channel("1", "Same", 3);
        when(channelRepository.findById("1")).thenReturn(Optional.of(existing));
        when(channelRepository.findByName("Same")).thenReturn(Optional.of(existing));
        when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("Same", service.update("1", request("Same")).getName());
    }

    @Test
    void update_nameOfAnotherChannel_throwsConflict() {
        when(channelRepository.findById("1")).thenReturn(Optional.of(channel("1", "Old", 0)));
        when(channelRepository.findByName("Other")).thenReturn(Optional.of(channel("2", "Other", 0)));

        assertThrows(ConflictException.class, () -> service.update("1", request("Other")));
    }

    @Test
    void update_unknownId_throwsNotFound() {
        when(channelRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update("zzz", request("X")));
    }

    @Test
    void delete_channelWithStreams_throwsConflict() {
        when(channelRepository.existsById("1")).thenReturn(true);
        when(streamRepository.existsByChannelId("1")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.delete("1"));
        verify(channelRepository, never()).deleteById(any());
    }

    @Test
    void delete_channelWithoutStreams_deletesIt() {
        when(channelRepository.existsById("1")).thenReturn(true);
        when(streamRepository.existsByChannelId("1")).thenReturn(false);

        service.delete("1");

        verify(channelRepository).deleteById("1");
    }

    @Test
    void delete_unknownId_throwsNotFound() {
        when(channelRepository.existsById("zzz")).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.delete("zzz"));
    }
}
