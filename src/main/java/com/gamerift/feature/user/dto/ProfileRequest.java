package com.gamerift.feature.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body of PUT /api/users/me/profile (Settings > Profile). The username and email cannot be changed here. */
@Getter
@Setter
@NoArgsConstructor
public class ProfileRequest {

    @Size(max = 120, message = "Bio can have at most 120 characters")
    private String bio;

    @NotBlank(message = "Country is required")
    @Size(max = 56, message = "Country can have at most 56 characters")
    private String country;
}