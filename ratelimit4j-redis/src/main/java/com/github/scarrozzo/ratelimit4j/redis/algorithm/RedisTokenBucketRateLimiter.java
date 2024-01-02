package com.github.scarrozzo.ratelimit4j.redis.algorithm;

import com.github.scarrozzo.ratelimit4j.core.algorithm.TokenBucketRateLimiter;
import com.github.scarrozzo.ratelimit4j.core.config.TokenBucketRateLimiterConfig;
import com.github.scarrozzo.ratelimit4j.core.exception.RateLimiterException;
import org.redisson.api.*;
import org.redisson.transaction.TransactionException;

import java.time.Duration;

public class RedisTokenBucketRateLimiter extends TokenBucketRateLimiter {
    private final RedissonClient redissonClient;
    private final TransactionOptions transactionOptions;

    public RedisTokenBucketRateLimiter(TokenBucketRateLimiterConfig tokenBucketRateLimiterConfig,
                                       RedissonClient redissonClient,
                                       TransactionOptions transactionOptions){
        super(tokenBucketRateLimiterConfig);
        this.redissonClient = redissonClient;
        this.transactionOptions = transactionOptions;
    }

    @Override
    public void evaluateRequest(String key) throws RateLimiterException {
        RLock lock = redissonClient.getLock(key.replaceAll("[^\\p{L}\\p{N}]", ""));
        lock.lock();

        RTransaction transaction = redissonClient.createTransaction(transactionOptions);
        try {
            RBucket<Long> tokenCount = transaction.getBucket(key);

            if(tokenCount.get() == null){
                tokenCount.set(rateLimiterConfig.getBucketSize()-1L,
                        Duration.ofMillis(rateLimiterConfig.getRefillPeriodInMilliSeconds()));
            } else if(tokenCount.get() == 0L){
                throw new RateLimiterException();
            } else {
                tokenCount.setAndKeepTTL(tokenCount.get()-1L);
            }

            transaction.commit();
        } catch(TransactionException|RateLimiterException e) {
            transaction.rollback();
            throw e;
        } finally {
            lock.unlock();
        }
    }
}
