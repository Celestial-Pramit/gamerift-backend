package com.gamerift.feature.ticker.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** A message in the scrolling bar at the bottom of the site (collection "ticker_messages"). */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "ticker_messages")
public class TickerMessage {

    @Id
    private String id;

    private String text;
    private boolean active = true;
}
