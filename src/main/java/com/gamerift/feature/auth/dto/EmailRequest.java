package com.gamerift.feature.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body that only contains an e-mail (resend code, forgot password). */
@Getter
@Setter
@NoArgsConstructor
public class EmailRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email is not valid")
    private String email;
}
