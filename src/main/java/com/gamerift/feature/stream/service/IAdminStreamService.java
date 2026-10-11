package com.gamerift.feature.stream.service;

import java.util.List;

import com.gamerift.feature.stream.dto.StreamRequest;
import com.gamerift.feature.stream.dto.StreamResponse;

/** Admin side of streams: create, update, delete and the quick switches (live / published). */
public interface IAdminStreamService {

    /** Every stream, drafts included, newest first. */
    List<StreamResponse> getAll();

    StreamResponse create(StreamRequest request);

    StreamResponse update(String id, StreamRequest request);

    /** Switch "Live now": true starts the stream, false ends it. A draft cannot go live. */
    StreamResponse setLive(String id, boolean live);

    /** Switch "Published". Un-publishing also removes the featured flag. */
    StreamResponse setPublished(String id, boolean published);

    void delete(String id);
}
