package com.example.userservice.exception;

public class PaymentCardNotFoundException extends RuntimeException {
    public PaymentCardNotFoundException(String message) {
        super(message);
    }
}