package com.gamerift.shared.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Called when a logged-in user asks for something their role does not allow: answer 403 JSON. */
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        JsonErrorWriter.write(response, 403, "Forbidden", "You do not have permission to do this", request.getRequestURI());
    }
}
