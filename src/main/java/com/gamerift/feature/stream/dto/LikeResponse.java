package com.gamerift.feature.stream.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** The like button state: does the logged-in user like it, and how many likes in total. */
@Getter
@AllArgsConstructor
public class LikeResponse {

    private boolean liked;
    private int likesCount;
}
