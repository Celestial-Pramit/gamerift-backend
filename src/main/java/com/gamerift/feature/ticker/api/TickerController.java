package com.gamerift.feature.ticker.api;

import java.util.List;

import com.gamerift.feature.ticker.dto.TickerItemResponse;
import com.gamerift.feature.ticker.service.ITickerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller (layer: HTTP) for the scrolling ticker bar.
 * Request flow: Browser -> TickerController -> ITickerService (TickerServiceImpl)
 *   -> ChannelRepository + GameEventRepository + TickerMessageRepository -> MongoDB -> List of TickerItemResponse JSON.
 */
@RestController
@RequestMapping("/api/ticker")
@Tag(name = "Ticker", description = "Scrolling ticker feed")
public class TickerController {

    private final ITickerService tickerService;

    public TickerController(ITickerService tickerService) {
        this.tickerService = tickerService;
    }

    @GetMapping
    @Operation(summary = "Ticker feed: live channels, events and admin messages")
    public List<TickerItemResponse> getFeed() {
        return tickerService.getFeed();
    }
}
