package com.gamerift.feature.stream.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of PUT /api/streams/{id}/like: {"liked": true} or {"liked": false}. */
@Getter
@Setter
@NoArgsConstructor
public class LikeRequest {

    private boolean liked;
}
