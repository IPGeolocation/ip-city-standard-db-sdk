package io.ipgeolocation.models.db;

import com.maxmind.db.MaxMindDbConstructor;
import com.maxmind.db.MaxMindDbParameter;

public class IPCityPlace {

  private final String connectionType;
  private final LocationPlace location;
  private final String timeZone;

  @MaxMindDbConstructor
  public IPCityPlace(
          @MaxMindDbParameter(name = "connection_type") String connectionType,
          @MaxMindDbParameter(name = "location") LocationPlace location,
          @MaxMindDbParameter(name = "time_zone") String timeZone
  ) {
    this.connectionType = connectionType;
    this.location = location;
    this.timeZone = timeZone;
  }

  public String getConnectionType() {
    return connectionType;
  }

  public LocationPlace getLocation() {
    return location;
  }

  public String getTimeZone() {
    return timeZone;
  }
}