package io.github.scarrozzo.ratelimit4j.core.algorithm;

import io.github.scarrozzo.ratelimit4j.core.config.RateLimiterConfig;
import io.github.scarrozzo.ratelimit4j.core.exception.RateLimiterConfigException;
import io.github.scarrozzo.ratelimit4j.core.exception.RateLimiterException;
import lombok.Getter;

public abstract class RateLimiter<T extends RateLimiterConfig> {

    @Getter
    protected T rateLimiterConfig;

    protected RateLimiter(T rateLimiterConfig) {
        if (rateLimiterConfig == null) {
            throw new RateLimiterConfigException("Rate limiter config cannot be null");
        }

        this.rateLimiterConfig = rateLimiterConfig;
    }


    public abstract void evaluateRequest(String key) throws RateLimiterException;

}
