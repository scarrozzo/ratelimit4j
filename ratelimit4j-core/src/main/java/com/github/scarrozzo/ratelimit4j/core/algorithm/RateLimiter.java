package com.github.scarrozzo.ratelimit4j.core.algorithm;

import com.github.scarrozzo.ratelimit4j.core.config.RateLimiterConfig;
import com.github.scarrozzo.ratelimit4j.core.exception.RateLimiterConfigException;
import com.github.scarrozzo.ratelimit4j.core.exception.RateLimiterException;
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
