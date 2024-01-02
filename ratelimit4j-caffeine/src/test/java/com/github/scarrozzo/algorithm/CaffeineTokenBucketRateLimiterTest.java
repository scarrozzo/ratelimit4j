package com.github.scarrozzo.algorithm;

import com.github.scarrozzo.ratelimit4j.caffeine.algorithm.CaffeineTokenBucketRateLimiter;
import com.github.scarrozzo.ratelimit4j.core.algorithm.RateLimiter;
import com.github.scarrozzo.ratelimit4j.core.config.TokenBucketRateLimiterConfig;
import com.github.scarrozzo.ratelimit4j.core.exception.RateLimiterException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.stream.IntStream;

class CaffeineTokenBucketRateLimiterTest {

    private static final String IP_ADDRESS_KEY = "127.0.0.1";

    @Test
    void evaluateRequestWithSuccess() {
        final RateLimiter<TokenBucketRateLimiterConfig> rateLimiter = new CaffeineTokenBucketRateLimiter(new TokenBucketRateLimiterConfig(2, 2_000L));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
    }

    @Test
    void evaluateRequestWithRateLimitError() {
        final RateLimiter<TokenBucketRateLimiterConfig> rateLimiter = new CaffeineTokenBucketRateLimiter(new TokenBucketRateLimiterConfig(2, 2_000L));

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
    }

    @Test
    @SneakyThrows
    void evaluateRequestWithSuccessAfterRefillPeriod() {
        long refillPeriodMsec = 200L;
        final RateLimiter<TokenBucketRateLimiterConfig> rateLimiter = new CaffeineTokenBucketRateLimiter(new TokenBucketRateLimiterConfig(2, refillPeriodMsec));

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));

        Thread.sleep(refillPeriodMsec);

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
    }

    @Test
    @SneakyThrows
    void evaluateRequestExpirationNotUpdatedAfterInsert() {
        long refillPeriodMsec = 200L;
        final RateLimiter<TokenBucketRateLimiterConfig> rateLimiter = new CaffeineTokenBucketRateLimiter(new TokenBucketRateLimiterConfig(2, refillPeriodMsec));

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Thread.sleep(refillPeriodMsec / 3);
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));

        Thread.sleep(refillPeriodMsec * 2 / 3);

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
    }

    @Test
    void verifyManyRequestsConcurrentUpdatesWithSuccess() {
        long refillPeriodMsec = 200L;
        long bucketSize = 10_000;
        final RateLimiter<TokenBucketRateLimiterConfig> rateLimiter = new CaffeineTokenBucketRateLimiter(new TokenBucketRateLimiterConfig(bucketSize, refillPeriodMsec));

        IntStream.range(0, (int) bucketSize).parallel().forEach(value -> {
            Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
            System.out.println("Finished " + value + ". time: " + Instant.now());
        });
    }

    @Test
    void verifyManyRequestsLimitError() {
        long refillPeriodMsec = 1000L;
        long bucketSize = 10_000;
        final RateLimiter<TokenBucketRateLimiterConfig> rateLimiter = new CaffeineTokenBucketRateLimiter(new TokenBucketRateLimiterConfig(bucketSize, refillPeriodMsec));

        Assertions.assertThrows(RateLimiterException.class, () ->
                IntStream.range(0, (int) bucketSize + 1).parallel().forEach(value -> {
                    rateLimiter.evaluateRequest(IP_ADDRESS_KEY);
                    System.out.println("Finished " + value + ". time: " + Instant.now());
                }));
    }
}
