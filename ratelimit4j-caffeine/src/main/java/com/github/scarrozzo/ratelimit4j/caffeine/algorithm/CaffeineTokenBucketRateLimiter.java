package com.github.scarrozzo.ratelimit4j.caffeine.algorithm;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.scarrozzo.ratelimit4j.core.algorithm.TokenBucketRateLimiter;
import com.github.scarrozzo.ratelimit4j.core.config.TokenBucketRateLimiterConfig;
import com.github.scarrozzo.ratelimit4j.core.exception.RateLimiterException;

public class CaffeineTokenBucketRateLimiter extends TokenBucketRateLimiter {

    private final Cache<String, Long> cache;

    public CaffeineTokenBucketRateLimiter(TokenBucketRateLimiterConfig rateLimiterConfig) {
        super(rateLimiterConfig);

        this.cache = Caffeine.newBuilder()
                .expireAfter(new Expiry<String, Long>() {
                    @Override
                    public long expireAfterCreate(String key, Long tokenCount, long currentTime) {
                        return rateLimiterConfig.getRefillPeriodInMilliSeconds() * 1_000_000;
                    }

                    @Override
                    public long expireAfterUpdate(String key, Long bucketState, long currentTime, long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(String key, Long bucketState, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    @Override
    public void evaluateRequest(String key) throws RateLimiterException {
        cache.asMap().compute(key, (k, tokenCount) -> {
            System.out.println(tokenCount);

            if (tokenCount == null) {
                return rateLimiterConfig.getBucketSize() - 1L;
            }

            if (tokenCount == 0) {
                throw new RateLimiterException();
            } else {
                return --tokenCount;
            }
        });
    }

}
