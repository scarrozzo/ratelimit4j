package com.github.scarrozzo.ratelimit4j.core.config;

import com.github.scarrozzo.ratelimit4j.core.exception.RateLimiterConfigException;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class FixedWindowCounterRateLimiterConfig extends RateLimiterConfig {

    private final long windowSizeInMilliseconds;
    private final long numberOfRequests;

    public FixedWindowCounterRateLimiterConfig(final long windowSizeInMilliseconds,
                                               final long numberOfRequests) {
        if (windowSizeInMilliseconds <= 0) {
            throw new RateLimiterConfigException("Invalid window size. Window size should be greater than zero");
        }

        if (numberOfRequests <= 0) {
            throw new RateLimiterConfigException("Invalid number of requests. Number of requests should be greater than zero");
        }

        this.windowSizeInMilliseconds = windowSizeInMilliseconds;
        this.numberOfRequests = numberOfRequests;
        this.rateLimiterType = RateLimiterType.FIXED_WINDOW_COUNTER;
    }

}
