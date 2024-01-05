package io.github.scarrozzo.algorithm;

import io.github.scarrozzo.ratelimit4j.caffeine.algorithm.CaffeineFixedWindowCounterRateLimiter;
import io.github.scarrozzo.ratelimit4j.core.algorithm.RateLimiter;
import io.github.scarrozzo.ratelimit4j.core.config.FixedWindowCounterRateLimiterConfig;
import io.github.scarrozzo.ratelimit4j.core.exception.RateLimiterException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.stream.IntStream;

class CaffeineFixedWindowCounterRateLimiterTest {

    private static final String IP_ADDRESS_KEY = "127.0.0.1";

    @Test
    void evaluateRequestWithSuccess() {
        final RateLimiter<FixedWindowCounterRateLimiterConfig> rateLimiter = new CaffeineFixedWindowCounterRateLimiter(
                new FixedWindowCounterRateLimiterConfig(500L, 1L));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
    }

    @Test
    void evaluateRequestWithRateLimitError() {
        final RateLimiter<FixedWindowCounterRateLimiterConfig> rateLimiter = new CaffeineFixedWindowCounterRateLimiter(
                new FixedWindowCounterRateLimiterConfig(500L, 2L));

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
    }

    @Test
    @SneakyThrows
    void evaluateRequestWithSuccessAfterWindowSize() {
        long windowSize = 200L;
        final RateLimiter<FixedWindowCounterRateLimiterConfig> rateLimiter = new CaffeineFixedWindowCounterRateLimiter(
                new FixedWindowCounterRateLimiterConfig(windowSize, 2L));

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));

        Thread.sleep(windowSize);

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
    }

    @Test
    @SneakyThrows
    void evaluateRequestExpirationNotUpdatedAfterInsert() {
        long windowSize = 200L;
        final RateLimiter<FixedWindowCounterRateLimiterConfig> rateLimiter = new CaffeineFixedWindowCounterRateLimiter(
                new FixedWindowCounterRateLimiterConfig(windowSize, 2L));

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Thread.sleep(windowSize / 3);
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));

        Thread.sleep(windowSize * 2 / 3);

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
    }

    @Test
    void verifyManyRequestsConcurrentUpdatesWithSuccess() {
        long windowSize = 200L;
        long numberOfRequests = 10_000L;
        final RateLimiter<FixedWindowCounterRateLimiterConfig> rateLimiter = new CaffeineFixedWindowCounterRateLimiter(
                new FixedWindowCounterRateLimiterConfig(windowSize, numberOfRequests));

        IntStream.range(0, (int) numberOfRequests).parallel().forEach(value -> {
            Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
            System.out.println("Finished " + value + ". time: " + Instant.now());
        });
    }

    @Test
    void verifyManyRequestsLimitError() {
        long windowSize = 1000L;
        long numberOfRequests = 10_000;
        final RateLimiter<FixedWindowCounterRateLimiterConfig> rateLimiter = new CaffeineFixedWindowCounterRateLimiter(
                new FixedWindowCounterRateLimiterConfig(windowSize, numberOfRequests));

        Assertions.assertThrows(RateLimiterException.class, () ->
                IntStream.range(0, (int) numberOfRequests + 1).parallel().forEach(value -> {
                    rateLimiter.evaluateRequest(IP_ADDRESS_KEY);
                    System.out.println("Finished " + value + ". time: " + Instant.now());
                }));
    }
}
