package com.gamerift.feature.channel.service;

import java.util.List;

import com.gamerift.feature.channel.dto.AdminChannelResponse;
import com.gamerift.feature.channel.dto.ChannelRequest;

/** Admin management of channels (create, update, delete). Public reading and follow stay in IChannelService. */
public interface IAdminChannelService {

    /** All channels, sorted by name. */
    List<AdminChannelResponse> getAll();

    /** Creates a channel. Throws 409 if the name is already used. */
    AdminChannelResponse create(ChannelRequest request);

    /** Updates a channel. Throws 404 if missing, 409 if the new name belongs to another channel. */
    AdminChannelResponse update(String id, ChannelRequest request);

    /** Deletes a channel. Throws 404 if missing, 409 if streams still belong to it. */
    void delete(String id);
}
