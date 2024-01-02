package com.github.scarrozzo.ratelimit4j.core.algorithm;

import com.github.scarrozzo.ratelimit4j.core.config.FixedWindowCounterRateLimiterConfig;

public abstract class FixedWindowCounterRateLimiter extends RateLimiter<FixedWindowCounterRateLimiterConfig> {
    protected FixedWindowCounterRateLimiter(FixedWindowCounterRateLimiterConfig fixedWindowCounterRateLimiterConfig) {
        super(fixedWindowCounterRateLimiterConfig);
    }
}
