package com.gamerift.feature.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of the admin create / update channel requests. followersCount is NOT here: only follows change it. */
@Getter
@Setter
@NoArgsConstructor
public class ChannelRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 40, message = "Name can have at most 40 characters")
    private String name;

    private boolean verified;
    private boolean pro;
    private boolean live;

    @Size(max = 60, message = "Note can have at most 60 characters")
    private String note;
}
