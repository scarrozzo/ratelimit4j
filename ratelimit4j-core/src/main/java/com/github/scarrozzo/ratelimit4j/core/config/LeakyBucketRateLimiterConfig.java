package com.github.scarrozzo.ratelimit4j.core.config;

import com.github.scarrozzo.ratelimit4j.core.exception.RateLimiterConfigException;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class LeakyBucketRateLimiterConfig extends RateLimiterConfig {

    private final long bucketSize;
    private final long outflowRateNumReq;
    private final long outflowRatePeriodInMilliseconds;
    private final long clearQueueAfterInactivityInMilliseconds;

    public LeakyBucketRateLimiterConfig(final long bucketSize,
                                        final long outflowRateNumReq,
                                        final long outflowRatePeriodInMilliseconds,
                                        final long clearQueueAfterInactivityInMilliseconds) {
        if (bucketSize <= 0) {
            throw new RateLimiterConfigException("Invalid bucket size. Bucket size should be greater than zero");
        }

        if (outflowRateNumReq <= 0) {
            throw new RateLimiterConfigException("Invalid outflow rate number of requests. Outflow rate number of requests should be greater than zero");
        }

        if (outflowRatePeriodInMilliseconds <= 0) {
            throw new RateLimiterConfigException("Invalid outflow rate period. Outflow rate period should be freater than zero");
        }

        if (clearQueueAfterInactivityInMilliseconds <= 0) {
            throw new RateLimiterConfigException("Invalid clear queue period. it should be greater than zero");
        }

        this.bucketSize = bucketSize;
        this.outflowRateNumReq = outflowRateNumReq;
        this.outflowRatePeriodInMilliseconds = outflowRatePeriodInMilliseconds;
        this.clearQueueAfterInactivityInMilliseconds = clearQueueAfterInactivityInMilliseconds;
        this.rateLimiterType = RateLimiterType.LEAKY_BUCKET;
    }

}
