package io.ipgeolocation.models.db;

import com.maxmind.db.MaxMindDbConstructor;
import com.maxmind.db.MaxMindDbParameter;

public class CityPlace {
  private final Place name;

  @MaxMindDbConstructor
  public CityPlace(@MaxMindDbParameter(name = "name") Place name) {
    this.name = name;
  }

  public Place getName() { return name; }
}
