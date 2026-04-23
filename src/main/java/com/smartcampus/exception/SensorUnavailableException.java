package com.smartcampus.exception;

/**
 * Thrown when a write is attempted against a Sensor that is not
 * currently available to record data (e.g. status MAINTENANCE or
 * OFFLINE). Maps to HTTP 403 Forbidden.
 */
public class SensorUnavailableException extends RuntimeException {

    public SensorUnavailableException(String message) {
        super(message);
    }
}
