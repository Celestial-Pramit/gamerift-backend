package com.gamerift.feature.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body of POST /api/auth/register.
 * The annotations are Jakarta Bean Validation rules: with @Valid in the controller, a bad request
 * is rejected automatically (400 + a message per field) before our service code even runs.
 */
@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be 3 to 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Username can only contain letters, numbers and underscore")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is not valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be 8 to 64 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password needs at least one letter and one number")
    private String password;

    @NotBlank(message = "Country is required")
    private String country;
}
