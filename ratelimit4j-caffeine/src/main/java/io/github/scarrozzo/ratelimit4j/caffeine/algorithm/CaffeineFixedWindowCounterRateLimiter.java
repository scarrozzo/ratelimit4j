package io.github.scarrozzo.ratelimit4j.caffeine.algorithm;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.scarrozzo.ratelimit4j.core.algorithm.FixedWindowCounterRateLimiter;
import io.github.scarrozzo.ratelimit4j.core.config.FixedWindowCounterRateLimiterConfig;
import io.github.scarrozzo.ratelimit4j.core.exception.RateLimiterException;
import lombok.Getter;

import java.time.Duration;

public class CaffeineFixedWindowCounterRateLimiter extends FixedWindowCounterRateLimiter {

    private final Cache<String, WindowStatus> cache;

    public CaffeineFixedWindowCounterRateLimiter(FixedWindowCounterRateLimiterConfig fixedWindowCounterRateLimiterConfig) {
        super(fixedWindowCounterRateLimiterConfig);

        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(fixedWindowCounterRateLimiterConfig.getWindowSizeInMilliseconds()))
                .build();
    }

    @Override
    public void evaluateRequest(String key) throws RateLimiterException {
        cache.asMap().compute(key, (k, windowStatus) -> {
            // a window for this key still does not exist
            if (windowStatus == null) {
                return WindowStatus.of(System.currentTimeMillis(), 1L);
            }

            // we are inside an existing window
            if ((System.currentTimeMillis() - windowStatus.getTimestamp()) <= rateLimiterConfig.getWindowSizeInMilliseconds()) {
                // the number of requests for this window has been reached
                if ((windowStatus.getCounter() + 1L) > rateLimiterConfig.getNumberOfRequests()) {
                    throw new RateLimiterException();
                }
                // the request is successful
                else {
                    return windowStatus.incCounter();
                }
            }
            // old window is expired. We can define a new window
            else {
                return WindowStatus.of(System.currentTimeMillis(), 1L);
            }
        });
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
