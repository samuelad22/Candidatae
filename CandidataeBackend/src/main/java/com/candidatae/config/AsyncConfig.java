package com.candidatae.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables asynchronous event execution ({@code @Async}) for background tasks
 * such as first-daily-login email synchronization.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}

