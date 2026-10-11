package com.gamerift.feature.auth.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.gamerift.feature.auth.model.OtpPurpose;
import com.gamerift.shared.util.JsonText;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends e-mails through the Brevo REST API (HTTPS, port 443).
 * Why not SMTP? Some free hosts (including Render's free tier) block SMTP ports, but HTTPS always works.
 *
 * Two modes (app.mail.mode / MAIL_MODE):
 *   console = nothing is sent, the e-mail text is printed in the log (perfect for development)
 *   brevo   = really sends the e-mail (needs BREVO_API_KEY and a verified sender, MAIL_FROM_EMAIL)
 *
 * @Async = runs in a background thread, so the user does not wait for the e-mail provider.
 */
@Service
public class EmailServiceImpl implements IEmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    private final String mode;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public EmailServiceImpl(@Value("${app.mail.mode}") String mode,
                            @Value("${app.mail.brevo-api-key}") String apiKey,
                            @Value("${app.mail.from-email}") String fromEmail,
                            @Value("${app.mail.from-name}") String fromName) {
        this.mode = mode;
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    @Async
    @Override
    public void sendOtp(String to, String code, OtpPurpose purpose) {
        String subject = purpose == OtpPurpose.SIGNUP ? "Verify your Gamerift email" : "Your Gamerift login code";
        String text = "Your Gamerift code is " + code + ". It expires in 5 minutes. If you did not ask for it, ignore this email.";
        String html = "<p>Your Gamerift code is <b style=\"font-size:20px\">" + code + "</b></p><p>It expires in 5 minutes. If you did not ask for it, ignore this email.</p>";
        send(to, subject, text, html);
    }

    @Async
    @Override
    public void sendPasswordReset(String to, String link) {
        String text = "Open this link within 15 minutes to choose a new password: " + link + " . If you did not ask for it, ignore this email.";
        String html = "<p>Click the button within 15 minutes to choose a new password:</p><p><a href=\"" + link + "\">Reset my password</a></p><p>If you did not ask for it, ignore this email.</p>";
        send(to, "Reset your Gamerift password", text, html);
    }

    private void send(String to, String subject, String text, String html) {
        if (!"brevo".equalsIgnoreCase(mode)) {
            log.info("[DEV MAIL, not sent] to={} | {} | {}", to, subject, text);
            return;
        }
        if (apiKey == null || apiKey.isBlank() || fromEmail == null || fromEmail.isBlank()) {
            log.error("MAIL_MODE=brevo but BREVO_API_KEY or MAIL_FROM_EMAIL is empty: email NOT sent.");
            return;
        }
        // The request body is small, so we write the JSON by hand
        String json = "{\"sender\":{\"name\":\"" + JsonText.escape(fromName) + "\",\"email\":\"" + JsonText.escape(fromEmail) + "\"},"
                + "\"to\":[{\"email\":\"" + JsonText.escape(to) + "\"}],"
                + "\"subject\":\"" + JsonText.escape(subject) + "\","
                + "\"htmlContent\":\"" + JsonText.escape(html) + "\","
                + "\"textContent\":\"" + JsonText.escape(text) + "\"}";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BREVO_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.error("Brevo refused the email ({}): {}", response.statusCode(), response.body());
            } else {
                log.info("Email sent to {}", to);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Email sending was interrupted", e);
        } catch (IOException e) {
            log.error("Could not reach Brevo", e);
        }
    }
}
