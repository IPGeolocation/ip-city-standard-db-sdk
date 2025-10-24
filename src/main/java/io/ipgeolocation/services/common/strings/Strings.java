package io.ipgeolocation.services.common.strings;

import static com.google.common.base.Strings.isNullOrEmpty;

public class Strings {

  public static void checkNotEmptyOrNull(String s) {
    if (isNullOrEmpty(s)) {
      throw new NullPointerException("Pre-condition violated: provided string must not be empty or null.");
    }
  }

  public static void checkNotEmptyOrNull(String s, String errorMessage) {
    if (isNullOrEmpty(s)) {
      throw new NullPointerException(errorMessage);
    }
  }

}