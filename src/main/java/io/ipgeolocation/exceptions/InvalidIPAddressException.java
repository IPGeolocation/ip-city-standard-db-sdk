package io.ipgeolocation.exceptions;

public class InvalidIPAddressException extends RuntimeException {
  public InvalidIPAddressException(String message) {
    super(message);
  }
}
