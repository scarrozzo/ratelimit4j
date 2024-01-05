package io.github.scarrozzo.ratelimit4j.caffeine.algorithm;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.scarrozzo.ratelimit4j.core.algorithm.LeakyBucketRateLimiter;
import io.github.scarrozzo.ratelimit4j.core.config.LeakyBucketRateLimiterConfig;
import io.github.scarrozzo.ratelimit4j.core.exception.RateLimiterException;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class CaffeineLeakyBucketRateLimiter extends LeakyBucketRateLimiter {

    private final Cache<String, Long> cache;
    private final ConcurrentHashMap<String, TimerTask> timerTasks;

    public CaffeineLeakyBucketRateLimiter(LeakyBucketRateLimiterConfig rateLimiterConfig) {
        super(rateLimiterConfig);

        timerTasks = new ConcurrentHashMap<>();

        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(rateLimiterConfig.getClearQueueAfterInactivityInMilliseconds()))
                .build();
    }

    @Override
    public void evaluateRequest(String key) throws RuntimeException {
        cache.asMap().compute(key, (k, bucketAvailableSize) -> {
            long result;

            if (bucketAvailableSize == null) {
                result = rateLimiterConfig.getBucketSize() - 1L;
            } else if (bucketAvailableSize == 0) {
                throw new RateLimiterException();
            } else {
                result = --bucketAvailableSize;
            }

            timerTasks.computeIfAbsent(k, mapKey -> {
                TimerTask timerTask = new ScheduleOutflowTask(k);
                new Timer().scheduleAtFixedRate(timerTask,
                        rateLimiterConfig.getOutflowRatePeriodInMilliseconds(),
                        rateLimiterConfig.getOutflowRatePeriodInMilliseconds());
                return timerTask;
            });

            return result;
        });
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
            if (!cache.asMap().containsKey(key)) {
                // cache key is not present. Turn off the timer task
                cancel();
                timerTasks.remove(key);
                return;
            }

            cache.asMap().compute(key, (k, bucketAvailableSize) -> {
                // queue is not empty, remove requests from queue
                if (bucketAvailableSize != null && bucketAvailableSize < rateLimiterConfig.getBucketSize()) {
                    lastUpdate = System.currentTimeMillis();
                    return Math.min(bucketAvailableSize + rateLimiterConfig.getOutflowRateNumReq(), rateLimiterConfig.getBucketSize());
                }

                // queue is empty and not used for more than ClearQueueAfterInactivityInMilliseconds, we can turn off the timer task to free resources
                if (System.currentTimeMillis() - lastUpdate >= rateLimiterConfig.getClearQueueAfterInactivityInMilliseconds()) {
                    cancel();
                    timerTasks.remove(key);
                }

                return bucketAvailableSize;
            });
        }
    }

}
