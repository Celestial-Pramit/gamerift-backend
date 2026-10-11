package com.gamerift.feature.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Answer of a successful registration. */
@Getter
@AllArgsConstructor
public class RegisterResponse {

    private String email;
    private String message;
}
