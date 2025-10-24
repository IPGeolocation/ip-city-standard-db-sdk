package io.ipgeolocation.exceptions;

public class IPNotFoundException extends RuntimeException {
  public IPNotFoundException(String message) {
    super(message);
  }
}