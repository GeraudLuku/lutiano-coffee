package com.lutiano.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Switches on the {@code @Scheduled} jobs (points expiry, order timeouts, shift closing, search indexing) and {@code @Async}. */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {}
