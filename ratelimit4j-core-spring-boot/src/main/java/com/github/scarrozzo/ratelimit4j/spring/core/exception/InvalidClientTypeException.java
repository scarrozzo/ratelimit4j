package com.github.scarrozzo.ratelimit4j.spring.core.exception;

public class InvalidClientTypeException extends RuntimeException {

    public InvalidClientTypeException() {
        super();
    }

    public InvalidClientTypeException(String message) {
        super(message);
    }

    public InvalidClientTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}