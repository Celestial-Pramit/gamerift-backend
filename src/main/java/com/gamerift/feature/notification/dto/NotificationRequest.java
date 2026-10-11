package com.gamerift.feature.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of the admin broadcast: which type and which text. */
@Getter
@Setter
@NoArgsConstructor
public class NotificationRequest {

    @NotBlank(message = "Type is required")
    @Pattern(regexp = "(?i)live|event|reply|pay|follow|system", message = "Type must be live, event, reply, pay, follow or system")
    private String type;

    @NotBlank(message = "Message is required")
    @Size(max = 200, message = "A message can have at most 200 characters")
    private String title;
}