package com.smartcampus.exception;

/**
 * Thrown when a resource being created references another resource
 * (e.g. a Sensor references a Room) that cannot be found. Maps to
 * HTTP 422 Unprocessable Entity.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
