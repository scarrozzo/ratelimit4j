package com.github.scarrozzo.ratelimit4j.redis.algorithm;

import com.github.scarrozzo.ratelimit4j.core.algorithm.LeakyBucketRateLimiter;
import com.github.scarrozzo.ratelimit4j.core.config.LeakyBucketRateLimiterConfig;
import com.github.scarrozzo.ratelimit4j.core.exception.RateLimiterException;
import org.redisson.api.*;
import org.redisson.transaction.TransactionException;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class RedisLeakyBucketRateLimiter extends LeakyBucketRateLimiter {
    private final RedissonClient redissonClient;
    private final TransactionOptions transactionOptions;
    private final ConcurrentHashMap<String, TimerTask> timerTasks;

    public RedisLeakyBucketRateLimiter(LeakyBucketRateLimiterConfig leakyBucketRateLimiterConfig,
                                       RedissonClient redissonClient,
                                       TransactionOptions transactionOptions){
        super(leakyBucketRateLimiterConfig);
        this.redissonClient = redissonClient;
        this.transactionOptions = transactionOptions;
        this.timerTasks = new ConcurrentHashMap<>();
    }

    @Override
    public void evaluateRequest(String key) throws RateLimiterException {
        RLock lock = redissonClient.getLock(key.replaceAll("[^\\p{L}\\p{N}]", ""));
        lock.lock();

        RTransaction transaction = redissonClient.createTransaction(transactionOptions);
        try {
            RBucket<Long> bucketAvailableSize = transaction.getBucket(key);

            if(bucketAvailableSize.get() == null){
                bucketAvailableSize.set(rateLimiterConfig.getBucketSize()-1L,
                        Duration.ofMillis(rateLimiterConfig.getClearQueueAfterInactivityInMilliseconds()));
            } else if(bucketAvailableSize.get() == 0L){
                throw new RateLimiterException();
            } else {
                bucketAvailableSize.set(bucketAvailableSize.get()-1L,
                        Duration.ofMillis(rateLimiterConfig.getClearQueueAfterInactivityInMilliseconds()));
            }

            transaction.commit();

            timerTasks.computeIfAbsent(key, mapKey -> {
                TimerTask timerTask = new ScheduleOutflowTask(key);
                new Timer().scheduleAtFixedRate(timerTask,
                        rateLimiterConfig.getOutflowRatePeriodInMilliseconds(),
                        rateLimiterConfig.getOutflowRatePeriodInMilliseconds());
                return timerTask;
            });
        } catch(TransactionException|RateLimiterException e) {
            transaction.rollback();
            throw e;
        } finally {
            lock.unlock();
        }
    }

    private class ScheduleOutflowTask extends TimerTask {
        private final String key;
        private long lastUpdate;

        public ScheduleOutflowTask(String key) {
            this.key = key;
            this.lastUpdate = System.currentTimeMillis();
        }

        @Override
        public void run() {
            RLock lock = redissonClient.getLock(key.replaceAll("[^\\p{L}\\p{N}]", ""));
            lock.lock();

            RTransaction transaction = redissonClient.createTransaction(transactionOptions);
            try{
                RBucket<Long> bucketAvailableSize = transaction.getBucket(key);
                if(bucketAvailableSize.get() != null && bucketAvailableSize.get() < rateLimiterConfig.getBucketSize()) {
                    // queue is not empty, remove requests from queue
                    lastUpdate = System.currentTimeMillis();
                    bucketAvailableSize.set(Math.min(bucketAvailableSize.get() + rateLimiterConfig.getOutflowRateNumReq(), rateLimiterConfig.getBucketSize()));
                } else if(bucketAvailableSize.get() == null || (System.currentTimeMillis() - lastUpdate) >= rateLimiterConfig.getClearQueueAfterInactivityInMilliseconds()) {
                    // cache key is not present. Turn off the timer task
                    // or queue is empty and not used for more than ClearQueueAfterInactivityInMilliseconds, we can turn off the timer task to free resources
                    cancel();
                    timerTasks.remove(key);
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
}
