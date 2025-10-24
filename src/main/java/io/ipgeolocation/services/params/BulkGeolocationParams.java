package io.ipgeolocation.services.params;


import java.util.Objects;

public class BulkGeolocationParams extends GeolocationParams {
  private final String[] ipAddresses;

  private BulkGeolocationParams(String[] ipAddresses, String lang) {
    super(lang);
    this.ipAddresses = ipAddresses;
  }

  public String[] getIPAddresses() {
    return ipAddresses;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends GeolocationParamsBuilder<Builder> {
    private String[] ipAddresses;

    public Builder withIPAddresses(String[] ipAddresses) {
      if (Objects.isNull(ipAddresses) || ipAddresses.length == 0) {
        throw new IllegalArgumentException("IP addresses cannot be null or empty");
      }
      this.ipAddresses = ipAddresses;
      return this;
    }

    @Override
    public BulkGeolocationParams build() {
      return new BulkGeolocationParams(ipAddresses, lang);
    }
  }
}
