package com.github.scarrozzo.ratelimit4j.core.algorithm;

import com.github.scarrozzo.ratelimit4j.core.config.TokenBucketRateLimiterConfig;

public abstract class TokenBucketRateLimiter extends RateLimiter<TokenBucketRateLimiterConfig> {
    protected TokenBucketRateLimiter(TokenBucketRateLimiterConfig rateLimiterConfig) {
        super(rateLimiterConfig);
    }
}
