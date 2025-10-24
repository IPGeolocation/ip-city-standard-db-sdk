package io.ipgeolocation.services.params;


import java.util.Objects;

public class IPGeolocationParams extends GeolocationParams {
  private final String ipAddress;

  private IPGeolocationParams(String ipAddress, String lang) {
    super(lang);
    this.ipAddress = ipAddress;
  }

  public String getIPAddress() {
    return ipAddress;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends GeolocationParamsBuilder<Builder> {
    private String ipAddress;

    public Builder withIPAddress(String ipAddress) {
      if (Objects.isNull(ipAddress) || ipAddress.isEmpty()) {
        throw new IllegalArgumentException("IP address cannot be null or empty");
      }
      this.ipAddress = ipAddress;
      return this;
    }

    @Override
    public IPGeolocationParams build() {
      return new IPGeolocationParams(ipAddress, lang);
    }
  }
}
