package io.github.scarrozzo.ratelimit4j.core.config;

import io.github.scarrozzo.ratelimit4j.core.exception.RateLimiterConfigException;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public final class TokenBucketRateLimiterConfig extends RateLimiterConfig {

    private final long bucketSize;
    private final long refillPeriodInMilliSeconds;

    public TokenBucketRateLimiterConfig(final long bucketSize, final long refillPeriodInMilliSeconds) {
        if (bucketSize <= 0) {
            throw new RateLimiterConfigException("Invalid bucket size. Bucket size should be greater than zero");
        }

        if (refillPeriodInMilliSeconds <= 0) {
            throw new RateLimiterConfigException("Invalid refill period. Refill period should be greater than zero");
        }

        this.bucketSize = bucketSize;
        this.refillPeriodInMilliSeconds = refillPeriodInMilliSeconds;
        this.rateLimiterType = RateLimiterType.TOKEN_BUCKET;
    }

}
