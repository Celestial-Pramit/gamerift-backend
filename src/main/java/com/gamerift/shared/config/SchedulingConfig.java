package com.gamerift.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled jobs (for example the trend score job). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
