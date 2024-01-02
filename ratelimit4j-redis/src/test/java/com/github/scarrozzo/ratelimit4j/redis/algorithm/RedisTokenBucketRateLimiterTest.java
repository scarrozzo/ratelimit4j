package com.github.scarrozzo.ratelimit4j.redis.algorithm;

import com.github.scarrozzo.ratelimit4j.core.algorithm.RateLimiter;
import com.github.scarrozzo.ratelimit4j.core.config.TokenBucketRateLimiterConfig;
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
class RedisTokenBucketRateLimiterTest {
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
    void evaluateRequestWithSuccess()  {
        final RateLimiter<TokenBucketRateLimiterConfig> rateLimiter = new RedisTokenBucketRateLimiter(
                new TokenBucketRateLimiterConfig(1L, 2000L),
                redissonClient, TransactionOptions.defaults());
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
    }

    @Test
    void evaluateRequestWithRateLimitError() throws IOException {
        final RateLimiter<TokenBucketRateLimiterConfig> rateLimiter = new RedisTokenBucketRateLimiter(
                new TokenBucketRateLimiterConfig(2L, 2000L),
                redissonClient, TransactionOptions.defaults());

        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
        Assertions.assertThrows(RateLimiterException.class, () -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
    }

    @Test
    @SneakyThrows
    void evaluateRequestWithSuccessAfterRefillPeriod() {
        long refillPeriodMsec = 200L;
        final RateLimiter<TokenBucketRateLimiterConfig> rateLimiter = new RedisTokenBucketRateLimiter(
                new TokenBucketRateLimiterConfig(2L, refillPeriodMsec),
                redissonClient, TransactionOptions.defaults());

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
        final RateLimiter<TokenBucketRateLimiterConfig> rateLimiter = new RedisTokenBucketRateLimiter(
                new TokenBucketRateLimiterConfig(2L, refillPeriodMsec),
                redissonClient, TransactionOptions.defaults());

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
    void verifyManyRequestsConcurrentUpdatesWithSuccess() throws IOException {
        long refillPeriodMsec = 1000L;
        long bucketSize = 100L;
        final RateLimiter<TokenBucketRateLimiterConfig> rateLimiter = new RedisTokenBucketRateLimiter(
                new TokenBucketRateLimiterConfig(bucketSize, refillPeriodMsec),
                redissonClient, TransactionOptions.defaults());

        IntStream.range(0, (int) bucketSize).parallel().forEach(value -> {
            Assertions.assertDoesNotThrow(() -> rateLimiter.evaluateRequest(IP_ADDRESS_KEY));
            System.out.println("Finished " + value + ". time: " + Instant.now());
        });
    }

    @Test
    void verifyManyRequestsLimitError() {
        long refillPeriodMsec = 1000L;
        long bucketSize = 100L;
        final RateLimiter<TokenBucketRateLimiterConfig> rateLimiter = new RedisTokenBucketRateLimiter(
                new TokenBucketRateLimiterConfig(bucketSize, refillPeriodMsec),
                redissonClient, TransactionOptions.defaults());

        Assertions.assertThrows(RateLimiterException.class, () ->
                IntStream.range(0, (int) bucketSize + 1).parallel().forEach(value -> {
                    rateLimiter.evaluateRequest(IP_ADDRESS_KEY);
                    System.out.println("Finished " + value + ". time: " + Instant.now());
                }));
    }
}
