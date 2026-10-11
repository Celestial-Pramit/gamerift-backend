package com.gamerift.feature.stream.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of the quick on/off switches of the admin table: {"enabled": true}. */
@Getter
@Setter
@NoArgsConstructor
public class ToggleRequest {

    private boolean enabled;
}
