package io.ipgeolocation.services.params;

import java.util.Objects;

public abstract class GeolocationParams {
  protected final String lang;


  protected GeolocationParams(String lang) {
    this.lang = (Objects.isNull(lang) || lang.isEmpty()) ? "en" : lang;
  }

  public String getLang() {
    return lang;
  }

}
