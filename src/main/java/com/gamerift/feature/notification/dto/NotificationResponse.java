package com.gamerift.feature.notification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A notification as the bell and the Alerts page show it. time = epoch milliseconds. */
@Getter
@Setter
@NoArgsConstructor
public class NotificationResponse {

    private String id;
    private String type;
    private String title;
    private String to;
    private boolean read;
    private long time;
}