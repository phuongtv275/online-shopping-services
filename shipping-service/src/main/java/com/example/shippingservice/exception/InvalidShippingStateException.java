package com.example.shippingservice.exception;

public class InvalidShippingStateException extends RuntimeException {
    public InvalidShippingStateException(String message) {
        super(message);
    }
}
