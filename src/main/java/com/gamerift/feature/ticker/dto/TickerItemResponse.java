package com.gamerift.feature.ticker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** One entry of the scrolling ticker. type is "live", "event" or "message"; the frontend can colour each type. */
@Getter
@AllArgsConstructor
public class TickerItemResponse {

    private String type;
    private String text;
}
