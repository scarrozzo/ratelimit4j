package io.github.scarrozzo.algorithm;

import io.github.scarrozzo.ratelimit4j.caffeine.algorithm.CaffeineLeakyBucketRateLimiter;
import io.github.scarrozzo.ratelimit4j.core.algorithm.RateLimiter;
import io.github.scarrozzo.ratelimit4j.core.config.LeakyBucketRateLimiterConfig;
import io.github.scarrozzo.ratelimit4j.core.exception.RateLimiterException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.stream.IntStream;

class CaffeineLeakyBucketRateLimiterTest {

    private static final String IP_ADDRESS_KEY = "127.0.0.1";

    @Test
    @SneakyThrows
    void evaluateRequestWithSuccess() {
        final RateLimiter<LeakyBucketRateLimiterConfig> rateLimiter = new CaffeineLeakyBucketRateLimiter(
                new LeakyBucketRateLimiterConfig(1L, 1L, 10L, 1000L));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Thread.sleep(200L);
    }

    @Test
    void evaluateRequestWithRateLimitError() {
        final RateLimiter<LeakyBucketRateLimiterConfig> rateLimiter = new CaffeineLeakyBucketRateLimiter(
                new LeakyBucketRateLimiterConfig(2L, 1L, 10L, 1000L));

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
    }

    @Test
    @SneakyThrows
    void evaluateRequestWithSuccessAfterOutflowPeriod() {
        long outflowPeriodMsec = 300L;
        final RateLimiter<LeakyBucketRateLimiterConfig> rateLimiter = new CaffeineLeakyBucketRateLimiter(
                new LeakyBucketRateLimiterConfig(2L, 2L, outflowPeriodMsec, 1000L));

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));

        Thread.sleep(outflowPeriodMsec + 100L);

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
    }

    @Test
    @SneakyThrows
    void evaluateRequestWithErrorAfterOutflowPeriod() {
        long outflowPeriodMsec = 300L;
        final RateLimiter<LeakyBucketRateLimiterConfig> rateLimiter = new CaffeineLeakyBucketRateLimiter(
                new LeakyBucketRateLimiterConfig(2L, 1L, outflowPeriodMsec, 1000L));

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));

        Thread.sleep(outflowPeriodMsec + 100L);

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
    }

    @Test
    void verifyManyRequestsConcurrentUpdatesWithSuccess() {
        long outflowPeriodMsec = 200L;
        long bucketSize = 10_000;
        final RateLimiter<LeakyBucketRateLimiterConfig> rateLimiter = new CaffeineLeakyBucketRateLimiter(
                new LeakyBucketRateLimiterConfig(bucketSize, 1L, outflowPeriodMsec, 1000L));

        IntStream.range(0, (int) bucketSize).parallel().forEach(value -> {
            Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
            System.out.println("Finished " + value + ". time: " + Instant.now());
        });
    }

    @Test
    void verifyManyRequestsLimitError() {
        long outflowPeriodMsec = 1000L;
        long bucketSize = 10_000;
        final RateLimiter<LeakyBucketRateLimiterConfig> rateLimiter = new CaffeineLeakyBucketRateLimiter(
                new LeakyBucketRateLimiterConfig(bucketSize, 1L, outflowPeriodMsec, 1000L));

        Assertions.assertThrows(RateLimiterException.class, () ->
                IntStream.range(0, (int) bucketSize + 1).parallel().forEach(value -> {
                    rateLimiter.evaluateRequest(IP_ADDRESS_KEY);
                    System.out.println("Finished " + value + ". time: " + Instant.now());
                }));
    }

    @Test
    @SneakyThrows
    void verifyManyRequestsConcurrentUpdatesWithSuccessAfterOutflowPeriod() {
        long outflowPeriodMsec = 400L;
        long bucketSize = 100;
        final RateLimiter<LeakyBucketRateLimiterConfig> rateLimiter = new CaffeineLeakyBucketRateLimiter(
                new LeakyBucketRateLimiterConfig(bucketSize, 10L, outflowPeriodMsec, 1000L));

        IntStream.range(0, (int) bucketSize).parallel().forEach(value -> {
            Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
            System.out.println("Finished " + value + ". time: " + Instant.now());
        });

        Thread.sleep(outflowPeriodMsec + 50L);

        IntStream.range(0, (int) 10L).parallel().forEach(value -> {
            Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
            System.out.println("Finished " + value + ". time: " + Instant.now());
        });
    }

    @Test
    @SneakyThrows
    void verifyManyRequestsConcurrentUpdatesWithErrorAfterOutflowPeriod() {
        long outflowPeriodMsec = 400L;
        long bucketSize = 100;
        final RateLimiter<LeakyBucketRateLimiterConfig> rateLimiter = new CaffeineLeakyBucketRateLimiter(
                new LeakyBucketRateLimiterConfig(bucketSize, 10L, outflowPeriodMsec, 1000L));

        IntStream.range(0, (int) bucketSize).parallel().forEach(value -> {
            Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
            System.out.println("Finished " + value + ". time: " + Instant.now());
        });

        Thread.sleep(outflowPeriodMsec + 50L);

        Assertions.assertThrows(RateLimiterException.class, () ->
                IntStream.range(0, (int) 11L).parallel().forEach(value -> {
                    rateLimiter.evaluateRequest(IP_ADDRESS_KEY);
                    System.out.println("Finished " + value + ". time: " + Instant.now());
                }));
    }

}
