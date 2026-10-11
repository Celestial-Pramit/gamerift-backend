package com.gamerift.feature.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The four alert switches of Settings > Alerts. Used as the request AND the response body.
 * live = followed channel goes live, events = event reminders, replies = replies to my comments, pay = payment receipts.
 * Phase 9/10 will check these switches before creating a notification.
 */
@Getter
@Setter
@NoArgsConstructor
public class NotificationPrefsDto {

    private boolean live = true;
    private boolean events = true;
    private boolean replies = true;
    private boolean pay = true;
}