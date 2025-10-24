package io.ipgeolocation.models.db;

import com.maxmind.db.MaxMindDbConstructor;
import com.maxmind.db.MaxMindDbParameter;

public class DistrictPlace {
  private final Place name;

  @MaxMindDbConstructor
  public DistrictPlace(@MaxMindDbParameter(name = "name") Place name) {
    this.name = name;
  }

  public Place getName() { return name; }
}
