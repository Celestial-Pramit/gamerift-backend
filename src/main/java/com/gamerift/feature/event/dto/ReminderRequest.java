package com.gamerift.feature.event.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of PUT /api/events/{id}/remind: {"enabled": true} or {"enabled": false}. */
@Getter
@Setter
@NoArgsConstructor
public class ReminderRequest {

    private boolean enabled;
}
