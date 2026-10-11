package com.gamerift.feature.ticker.service;

import java.util.List;

import com.gamerift.feature.ticker.dto.TickerItemResponse;
import com.gamerift.feature.ticker.dto.TickerMessageRequest;
import com.gamerift.feature.ticker.dto.TickerMessageResponse;

/** The ticker feature: the public feed (built from live data) and admin management of the written messages. */
public interface ITickerService {

    /** The feed: live channels first, then events (live now or starting within 24 hours), then active admin messages. */
    List<TickerItemResponse> getFeed();

    /** All admin messages including inactive ones. */
    List<TickerMessageResponse> getAllMessages();

    TickerMessageResponse create(TickerMessageRequest request);

    /** Throws 404 if the id does not exist. */
    TickerMessageResponse update(String id, TickerMessageRequest request);

    /** Throws 404 if the id does not exist. */
    void delete(String id);
}
