package com.gamerift.shared.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes the same JSON error shape as GlobalExceptionHandler, but straight to the response.
 * Needed because security errors happen in filters, BEFORE any controller (and before the controller advice).
 */
public final class JsonErrorWriter {

    private JsonErrorWriter() {
    }

    public static void write(HttpServletResponse response, int status, String error, String message, String path) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String json = "{\"status\":" + status + ",\"error\":\"" + escape(error) + "\",\"message\":\"" + escape(message)
                + "\",\"path\":\"" + escape(path) + "\"}";
        response.getWriter().write(json);
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
