package io.github.scarrozzo.ratelimit4j.core.exception;

public class RateLimiterException extends RuntimeException {

    public RateLimiterException() {
        super();
    }

    public RateLimiterException(String message) {
        super(message);
    }

    public RateLimiterException(String message, Throwable cause) {
        super(message, cause);
    }
}
