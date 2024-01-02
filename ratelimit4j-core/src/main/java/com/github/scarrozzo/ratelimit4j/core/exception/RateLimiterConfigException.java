package com.github.scarrozzo.ratelimit4j.core.exception;

public class RateLimiterConfigException extends RuntimeException {

    public RateLimiterConfigException() {
        super();
    }

    public RateLimiterConfigException(String message) {
        super(message);
    }

    public RateLimiterConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
