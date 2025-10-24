package io.ipgeolocation.models;

import com.maxmind.db.MaxMindDbConstructor;
import com.maxmind.db.MaxMindDbParameter;

public class CountryMetadata {
  private final String callingCode;
  private final String languages;
  private final String tld;

  @MaxMindDbConstructor
  public CountryMetadata(
          @MaxMindDbParameter(name = "calling_code") String callingCode,
          @MaxMindDbParameter(name = "languages") String languages,
          @MaxMindDbParameter(name = "tld") String tld
  ) {
    this.callingCode = callingCode;
    this.languages = languages;
    this.tld = tld;
  }

  public String getCallingCode() { return callingCode; }
  public String getLanguages() { return languages; }
  public String getTld() { return tld; }

  @Override
  public String toString() {
    return "CountryMetadata{" +
            "callingCode='" + callingCode + '\'' +
            ", languages='" + languages + '\'' +
            ", tld='" + tld + '\'' +
            '}';
  }
}