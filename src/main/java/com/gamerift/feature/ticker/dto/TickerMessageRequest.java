package com.gamerift.feature.ticker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of the admin create / update ticker message requests. */
@Getter
@Setter
@NoArgsConstructor
public class TickerMessageRequest {

    @NotBlank(message = "Text is required")
    @Size(max = 140, message = "Text can have at most 140 characters")
    private String text;

    private boolean active = true;
}
