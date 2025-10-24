package io.ipgeolocation.services.params;


public abstract class GeolocationParamsBuilder<T extends GeolocationParamsBuilder<T>> {
  protected String lang = "en";


  public T withLang(String lang) {
    this.lang = (lang == null || lang.isEmpty()) ? "en" : lang;
    return (T) this;
  }


  public abstract GeolocationParams build();
}
