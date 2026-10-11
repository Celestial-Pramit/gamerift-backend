package com.gamerift.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Turns on @Async: e-mails are sent in a background thread so the API answers immediately. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
