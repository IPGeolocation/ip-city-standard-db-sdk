package io.ipgeolocation.models;

import com.maxmind.db.MaxMindDbConstructor;
import com.maxmind.db.MaxMindDbParameter;

public class Coordinates {
  private final String latitude;
  private final String longitude;

  @MaxMindDbConstructor
  public Coordinates(
          @MaxMindDbParameter(name = "latitude") String latitude,
          @MaxMindDbParameter(name = "longitude") String longitude
  ) {
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public String getLatitude() { return latitude; }
  public String getLongitude() { return longitude; }

  public double getLatitudeAsDouble() {
    try { return Double.parseDouble(latitude); } catch (Exception e) { return Double.NaN; }
  }
  public double getLongitudeAsDouble() {
    try { return Double.parseDouble(longitude); } catch (Exception e) { return Double.NaN; }
  }

  @Override
  public String toString() {
    return "Coordinates{" +
            "latitude='" + latitude + '\'' +
            ", longitude='" + longitude + '\'' +
            '}';
  }
}