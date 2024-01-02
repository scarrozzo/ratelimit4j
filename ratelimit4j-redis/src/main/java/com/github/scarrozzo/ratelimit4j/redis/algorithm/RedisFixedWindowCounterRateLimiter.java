package com.github.scarrozzo.ratelimit4j.redis.algorithm;

import com.github.scarrozzo.ratelimit4j.core.algorithm.FixedWindowCounterRateLimiter;
import com.github.scarrozzo.ratelimit4j.core.config.FixedWindowCounterRateLimiterConfig;
import com.github.scarrozzo.ratelimit4j.core.exception.RateLimiterException;
import lombok.Getter;
import org.redisson.api.*;
import org.redisson.transaction.TransactionException;

public class RedisFixedWindowCounterRateLimiter extends FixedWindowCounterRateLimiter {

    private final RedissonClient redissonClient;
    private final TransactionOptions transactionOptions;

    public RedisFixedWindowCounterRateLimiter(FixedWindowCounterRateLimiterConfig fixedWindowCounterRateLimiterConfig,
                                       RedissonClient redissonClient,
                                       TransactionOptions transactionOptions){
        super(fixedWindowCounterRateLimiterConfig);
        this.redissonClient = redissonClient;
        this.transactionOptions = transactionOptions;
    }

    @Override
    public void evaluateRequest(String key) throws RateLimiterException {
        RLock lock = redissonClient.getLock(key.replaceAll("[^\\p{L}\\p{N}]", ""));
        lock.lock();

        RTransaction transaction = redissonClient.createTransaction(transactionOptions);
        try {
            RBucket<WindowStatus> windowStatus = transaction.getBucket(key);

            // a window for this key still does not exist
            if (windowStatus.get() == null) {
                windowStatus.set(WindowStatus.of(System.currentTimeMillis(), 1L));
            }
            // we are inside an existing window
            else if((System.currentTimeMillis() - windowStatus.get().getTimestamp()) <= rateLimiterConfig.getWindowSizeInMilliseconds()) {
                // the number of requests for this window has been reached
                if ((windowStatus.get().getCounter() + 1L) > rateLimiterConfig.getNumberOfRequests()) {
                    throw new RateLimiterException();
                }
                // the request is successful
                else {
                    windowStatus.set(windowStatus.get().incCounter());
                }
            }
            // old window is expired. We can define a new window
            else {
                windowStatus.set(WindowStatus.of(System.currentTimeMillis(), 1L));
            }

            transaction.commit();
        } catch(TransactionException |RateLimiterException e) {
            transaction.rollback();
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Getter
    private static class WindowStatus {
        private final long timestamp;
        private long counter;

        private WindowStatus(long timestamp, long counter) {
            this.timestamp = timestamp;
            this.counter = counter;
        }

        public static WindowStatus of(long timestamp, long counter) {
            return new WindowStatus(timestamp, counter);
        }

        public WindowStatus incCounter() {
            this.counter++;
            return this;
        }
    }
}
