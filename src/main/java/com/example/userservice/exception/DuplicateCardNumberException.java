package com.example.userservice.exception;

public class DuplicateCardNumberException extends RuntimeException {
  public DuplicateCardNumberException(String message) {
    super(message);
  }
}
