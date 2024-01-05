package io.github.scarrozzo.ratelimit4j.spring.caffeine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ratelimit4j.caffeine.leakybucket")
public class CaffeineLeakyBucketRateLimiterProperties {

    public static final long DEFAULT_BUCKET_SIZE = 10L;
    public static final long DEFAULT_OUTFLOW_RATE_NUM_REQ = 10;
    public static final long DEFAULT_OUTFLOW_RATE_PERIOD_IN_MILLISECS = 1_000L;
    public static final long DEFAULT_CLEAR_QUEUE_AFTER_INACTIVITY_IN_MILLISECS = 300_000L;

    private final Long bucketSize;
    private final Long outflowRateNumReq;
    private final Long outflowRatePeriodInMilliseconds;
    private final Long clearQueueAfterInactivityInMilliseconds;

}
