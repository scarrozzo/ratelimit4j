package io.github.scarrozzo.ratelimit4j.spring.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ratelimit4j.redis.tokenbucket")
public class RedisTokenBucketRateLimiterProperties {

    public static final long DEFAULT_BUCKET_SIZE = 10L;
    public static final long DEFAULT_REFILL_PERIOD_IN_MILLISECS = 1_000L;

    private Long bucketSize;
    private Long refillPeriodInMilliSeconds;

}
