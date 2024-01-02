package com.github.scarrozzo.ratelimit4j.redis.algorithm;

import com.github.scarrozzo.ratelimit4j.core.algorithm.RateLimiter;
import com.github.scarrozzo.ratelimit4j.core.config.FixedWindowCounterRateLimiterConfig;
import com.github.scarrozzo.ratelimit4j.core.exception.RateLimiterException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
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
class RedisFixedWindowCounterRateLimiterTest {

    private static final String IP_ADDRESS_KEY = "127.0.0.1";

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

    @BeforeEach
    public void beforeEach(){
        redissonClient.getBucket(IP_ADDRESS_KEY).delete();
    }

    @AfterAll
    public static void afterAll() {
        redisClient.shutdown();
    }

    @Test
    void evaluateRequestWithSuccess() {
        final RateLimiter<FixedWindowCounterRateLimiterConfig> rateLimiter = new RedisFixedWindowCounterRateLimiter(
                new FixedWindowCounterRateLimiterConfig(500L, 1L),
                redissonClient, TransactionOptions.defaults());
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
    }

    @Test
    void evaluateRequestWithRateLimitError() {
        final RateLimiter<FixedWindowCounterRateLimiterConfig> rateLimiter = new RedisFixedWindowCounterRateLimiter(
                new FixedWindowCounterRateLimiterConfig(500L, 2L),
                redissonClient, TransactionOptions.defaults());

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
    }

    @Test
    @SneakyThrows
    void evaluateRequestWithSuccessAfterWindowSize() {
        long windowSize = 200L;
        final RateLimiter<FixedWindowCounterRateLimiterConfig> rateLimiter = new RedisFixedWindowCounterRateLimiter(
                new FixedWindowCounterRateLimiterConfig(windowSize, 2L),
                redissonClient, TransactionOptions.defaults());

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
        final RateLimiter<FixedWindowCounterRateLimiterConfig> rateLimiter = new RedisFixedWindowCounterRateLimiter(
                new FixedWindowCounterRateLimiterConfig(windowSize, 2L),
                redissonClient, TransactionOptions.defaults());

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
        long numberOfRequests = 100L;
        final RateLimiter<FixedWindowCounterRateLimiterConfig> rateLimiter = new RedisFixedWindowCounterRateLimiter(
                new FixedWindowCounterRateLimiterConfig(windowSize, numberOfRequests),
                redissonClient, TransactionOptions.defaults());

        IntStream.range(0, (int) numberOfRequests).parallel().forEach(value -> {
            Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
            System.out.println("Finished " + value + ". time: " + Instant.now());
        });
    }

    @Test
    void verifyManyRequestsLimitError() {
        long windowSize = 1000L;
        long numberOfRequests = 100;
        final RateLimiter<FixedWindowCounterRateLimiterConfig> rateLimiter = new RedisFixedWindowCounterRateLimiter(
                new FixedWindowCounterRateLimiterConfig(windowSize, numberOfRequests),
                redissonClient, TransactionOptions.defaults());

        Assertions.assertThrows(RateLimiterException.class, () ->
                IntStream.range(0, (int) numberOfRequests + 1).parallel().forEach(value -> {
                    rateLimiter.evaluateRequest(IP_ADDRESS_KEY);
                    System.out.println("Finished " + value + ". time: " + Instant.now());
                }));
    }
}
