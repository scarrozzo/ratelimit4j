package com.github.scarrozzo.ratelimit4j.spring.caffeine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ratelimit4j.caffeine.fixedwindowcounter")
public class CaffeineFixedWindowCounterRateLimiterProperties {
    public static final long DEFAULT_NUMBER_OF_REQUESTS = 10L;
    public static final long DEFAULT_WINDOW_SIZE_IN_MILLISECS = 1_000L;

    private Long numberOfRequests;
    private Long windowSize;
}
