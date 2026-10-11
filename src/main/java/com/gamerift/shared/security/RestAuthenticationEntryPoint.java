package com.gamerift.shared.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** Called when someone who is NOT logged in asks for a protected URL: answer 401 JSON. */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        JsonErrorWriter.write(response, 401, "Unauthorized", "Login required: send a valid Bearer token", request.getRequestURI());
    }
}
