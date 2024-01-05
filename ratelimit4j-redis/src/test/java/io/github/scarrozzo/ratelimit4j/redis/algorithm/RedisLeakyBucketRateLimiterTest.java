package io.github.scarrozzo.ratelimit4j.redis.algorithm;

import io.github.scarrozzo.ratelimit4j.core.algorithm.RateLimiter;
import io.github.scarrozzo.ratelimit4j.core.config.LeakyBucketRateLimiterConfig;
import io.github.scarrozzo.ratelimit4j.core.exception.RateLimiterException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.TransactionOptions;
import org.redisson.client.RedisClient;
import org.redisson.client.RedisClientConfig;
import org.redisson.config.Config;
import org.redisson.config.Protocol;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.Instant;
import java.util.stream.IntStream;

@Testcontainers
class RedisLeakyBucketRateLimiterTest {

    @Container
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7.2")
                    .withExposedPorts(6379);
    private static RedisClient redisClient;
    private static RedissonClient redissonClient;

    @BeforeAll
    public static void beforeAll() throws IOException {
        RedisClientConfig config = new RedisClientConfig();
        config.setProtocol(Protocol.RESP3);
        config.setAddress("redis://127.0.0.1:" + REDIS.getFirstMappedPort());
        redisClient = RedisClient.create(config);
        redissonClient = Redisson.create(Config.fromYAML("""
                        singleServerConfig:
                            address: "redis://127.0.0.1:%s"
                        """.formatted(REDIS.getFirstMappedPort())));
    }

    @AfterAll
    public static void afterAll() {
        redisClient.shutdown();
    }

    @Test
    @SneakyThrows
    void evaluateRequestWithSuccess() {
        String key = "127.0.0.1";
        final RateLimiter<LeakyBucketRateLimiterConfig> rateLimiter = new RedisLeakyBucketRateLimiter(
                new LeakyBucketRateLimiterConfig(1L, 1L, 10L, 1000L),
                redissonClient, TransactionOptions.defaults());
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(key));
        Thread.sleep(200L);
    }

    @Test
    void evaluateRequestWithRateLimitError() {
        String key = "127.0.0.2";
        final RateLimiter<LeakyBucketRateLimiterConfig> rateLimiter = new RedisLeakyBucketRateLimiter(
                new LeakyBucketRateLimiterConfig(2L, 1L, 100L, 1000L),
                redissonClient, TransactionOptions.defaults());

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(key));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(key));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(key));
    }

    @Test
    @SneakyThrows
    void evaluateRequestWithSuccessAfterOutflowPeriod() {
        String key = "127.0.0.3";
        long outflowPeriodMsec = 300L;
        final RateLimiter<LeakyBucketRateLimiterConfig> rateLimiter = new RedisLeakyBucketRateLimiter(
                new LeakyBucketRateLimiterConfig(2L, 2L, outflowPeriodMsec, 1000L),
                redissonClient, TransactionOptions.defaults());

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(key));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(key));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(key));

        Thread.sleep(outflowPeriodMsec + 100L);

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(key));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(key));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(key));
    }

    @Test
    @SneakyThrows
    void evaluateRequestWithErrorAfterOutflowPeriod() {
        String key = "127.0.0.4";
        long outflowPeriodMsec = 300L;
        final RateLimiter<LeakyBucketRateLimiterConfig> rateLimiter = new RedisLeakyBucketRateLimiter(
                new LeakyBucketRateLimiterConfig(2L, 1L, outflowPeriodMsec, 1000L),
                redissonClient, TransactionOptions.defaults());

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(key));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(key));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(key));

        Thread.sleep(outflowPeriodMsec + 100L);

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(key));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(key));
    }

    @Test
    void verifyManyRequestsConcurrentUpdatesWithSuccess() {
        String key = "127.0.0.5";
        long outflowPeriodMsec = 200L;
        long bucketSize = 100;
        final RateLimiter<LeakyBucketRateLimiterConfig> rateLimiter = new RedisLeakyBucketRateLimiter(
                new LeakyBucketRateLimiterConfig(bucketSize, 1L, outflowPeriodMsec, 1000L),
                redissonClient, TransactionOptions.defaults());

        IntStream.range(0, (int) bucketSize).parallel().forEach(value -> {
            Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(key));
            System.out.println("Finished " + value + ". time: " + Instant.now());
        });
    }

    @Test
    void verifyManyRequestsLimitError() {
        String key = "127.0.0.6";
        long outflowPeriodMsec = 1000L;
        long bucketSize = 100;
        final RateLimiter<LeakyBucketRateLimiterConfig> rateLimiter = new RedisLeakyBucketRateLimiter(
                new LeakyBucketRateLimiterConfig(bucketSize, 1L, outflowPeriodMsec, 1000L),
                redissonClient, TransactionOptions.defaults());

        Assertions.assertThrows(RateLimiterException.class, () ->
                IntStream.range(0, (int) bucketSize + 1).parallel().forEach(value -> {
                    rateLimiter.evaluateRequest(key);
                    System.out.println("Finished " + value + ". time: " + Instant.now());
                }));
    }

    @Test
    @SneakyThrows
    void verifyManyRequestsConcurrentUpdatesWithSuccessAfterOutflowPeriod() {
        String key = "127.0.0.7";
        long outflowPeriodMsec = 400L;
        long bucketSize = 100;
        final RateLimiter<LeakyBucketRateLimiterConfig> rateLimiter = new RedisLeakyBucketRateLimiter(
                new LeakyBucketRateLimiterConfig(bucketSize, 10L, outflowPeriodMsec, 1000L),
                redissonClient, TransactionOptions.defaults());

        IntStream.range(0, (int) bucketSize).parallel().forEach(value -> {
            Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(key));
            System.out.println("Finished " + value + ". time: " + Instant.now());
        });

        Thread.sleep(outflowPeriodMsec + 50L);

        IntStream.range(0, (int) 10L).parallel().forEach(value -> {
            Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(key));
            System.out.println("Finished " + value + ". time: " + Instant.now());
        });
    }

    @Test
    @SneakyThrows
    void verifyManyRequestsConcurrentUpdatesWithErrorAfterOutflowPeriod() {
        String key = "127.0.0.8";
        long outflowPeriodMsec = 1000L;
        long bucketSize = 100;
        final RateLimiter<LeakyBucketRateLimiterConfig> rateLimiter = new RedisLeakyBucketRateLimiter(
                new LeakyBucketRateLimiterConfig(bucketSize, 10L, outflowPeriodMsec, 10000L),
                redissonClient, TransactionOptions.defaults());

        IntStream.range(0, (int) bucketSize).parallel().forEach(value -> {
            Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(key));
            System.out.println("Finished " + value + ". time: " + Instant.now());
        });

        Thread.sleep(outflowPeriodMsec + 50L);

        Assertions.assertThrows(RateLimiterException.class, () ->
                IntStream.range(0, (int) 11L).parallel().forEach(value -> {
                    rateLimiter.evaluateRequest(key);
                    System.out.println("Finished " + value + ". time: " + Instant.now());
                }));
    }

}
