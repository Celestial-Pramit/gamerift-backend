package com.gamerift.feature.event.dto;

import java.time.Instant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body of the admin create / update event requests.
 * Simple rules are annotations (@Valid); the rule "raised cannot be more than the goal" is in the service.
 */
@Getter
@Setter
@NoArgsConstructor
public class EventRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 120, message = "Title can have at most 120 characters")
    private String title;

    @NotBlank(message = "Category is required")
    @Pattern(regexp = "(?i)ESPORTS|IRL|SPEEDRUN|WORKSHOP", message = "Category must be ESPORTS, IRL, SPEEDRUN or WORKSHOP")
    private String category;

    @NotNull(message = "Start time is required")
    private Instant startsAt;

    @Size(max = 200, message = "Note can have at most 200 characters")
    private String note;

    // "", "soon", "fund" or "pro"
    @Pattern(regexp = "|soon|fund|pro", message = "Badge must be empty, soon, fund or pro")
    private String badge = "";

    @Min(value = 0, message = "Goal cannot be negative")
    private long goal;

    @Min(value = 0, message = "Raised cannot be negative")
    private long raised;
}
