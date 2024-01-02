package com.github.scarrozzo.ratelimit4j.core.config;


import lombok.Getter;

public abstract class RateLimiterConfig {
    @Getter
    protected RateLimiterType rateLimiterType;
}
