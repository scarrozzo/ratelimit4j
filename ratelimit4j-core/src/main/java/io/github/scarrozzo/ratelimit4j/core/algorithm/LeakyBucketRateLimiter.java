package io.github.scarrozzo.ratelimit4j.core.algorithm;

import io.github.scarrozzo.ratelimit4j.core.config.LeakyBucketRateLimiterConfig;

public abstract class LeakyBucketRateLimiter extends RateLimiter<LeakyBucketRateLimiterConfig> {
    protected LeakyBucketRateLimiter(LeakyBucketRateLimiterConfig leakyBucketRateLimiterConfig) {
        super(leakyBucketRateLimiterConfig);
    }
}
