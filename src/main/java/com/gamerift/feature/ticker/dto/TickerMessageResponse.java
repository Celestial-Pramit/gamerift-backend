package com.gamerift.feature.ticker.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An admin-written ticker message as the admin panel sees it. */
@Getter
@Setter
@NoArgsConstructor
public class TickerMessageResponse {

    private String id;
    private String text;
    private boolean active;
}
