package com.gamerift.feature.channel.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.gamerift.feature.channel.dto.AdminChannelResponse;
import com.gamerift.feature.channel.dto.ChannelRequest;
import com.gamerift.feature.channel.model.Channel;
import com.gamerift.feature.channel.repository.ChannelRepository;
import com.gamerift.feature.stream.repository.LiveStreamRepository;
import com.gamerift.shared.exception.ConflictException;
import com.gamerift.shared.exception.NotFoundException;

import org.springframework.stereotype.Service;

/**
 * Business rules for managing channels (layer: service).
 * Called by: AdminChannelController. Calls: ChannelRepository, LiveStreamRepository.
 * Rules: channel names are unique; a channel that still has streams cannot be deleted
 * (it would leave streams pointing at a channel that does not exist).
 */
@Service
public class AdminChannelServiceImpl implements IAdminChannelService {

    private final ChannelRepository channelRepository;
    private final LiveStreamRepository streamRepository;

    public AdminChannelServiceImpl(ChannelRepository channelRepository, LiveStreamRepository streamRepository) {
        this.channelRepository = channelRepository;
        this.streamRepository = streamRepository;
    }

    @Override
    public List<AdminChannelResponse> getAll() {
        List<Channel> channels = new ArrayList<>(channelRepository.findAll());
        channels.sort(Comparator.comparing(Channel::getName, String.CASE_INSENSITIVE_ORDER));
        List<AdminChannelResponse> result = new ArrayList<>();
        for (Channel channel : channels) {
            result.add(toResponse(channel));
        }
        return result;
    }

    @Override
    public AdminChannelResponse create(ChannelRequest request) {
        // 1. clean the name and check it is free
        String name = request.getName().trim();
        if (channelRepository.findByName(name).isPresent()) {
            throw new ConflictException("A channel with this name already exists");
        }
        // 2. build and save (a new channel starts with 0 followers)
        Channel channel = new Channel();
        channel.setFollowersCount(0);
        copyInto(channel, request, name);
        return toResponse(channelRepository.save(channel));
    }

    @Override
    public AdminChannelResponse update(String id, ChannelRequest request) {
        // 1. the channel must exist
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Channel not found"));
        // 2. the new name must not belong to ANOTHER channel
        String name = request.getName().trim();
        Optional<Channel> sameName = channelRepository.findByName(name);
        if (sameName.isPresent() && !sameName.get().getId().equals(id)) {
            throw new ConflictException("A channel with this name already exists");
        }
        // 3. overwrite and save (followersCount is kept)
        copyInto(channel, request, name);
        return toResponse(channelRepository.save(channel));
    }

    @Override
    public void delete(String id) {
        // 1. the channel must exist
        if (!channelRepository.existsById(id)) {
            throw new NotFoundException("Channel not found");
        }
        // 2. block the delete while streams belong to it
        if (streamRepository.existsByChannelId(id)) {
            throw new ConflictException("Delete or move this channel's streams first");
        }
        // 3. delete
        channelRepository.deleteById(id);
    }

    private void copyInto(Channel channel, ChannelRequest request, String name) {
        channel.setName(name);
        channel.setVerified(request.isVerified());
        channel.setPro(request.isPro());
        channel.setLive(request.isLive());
        channel.setNote(request.getNote());
    }

    private AdminChannelResponse toResponse(Channel channel) {
        AdminChannelResponse r = new AdminChannelResponse();
        r.setId(channel.getId());
        r.setName(channel.getName());
        r.setFollowersCount(channel.getFollowersCount());
        r.setVerified(channel.isVerified());
        r.setPro(channel.isPro());
        r.setLive(channel.isLive());
        r.setNote(channel.getNote());
        return r;
    }
}
