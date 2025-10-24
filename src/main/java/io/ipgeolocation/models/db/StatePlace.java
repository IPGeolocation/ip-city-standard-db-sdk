package io.ipgeolocation.models.db;

import com.maxmind.db.MaxMindDbConstructor;
import com.maxmind.db.MaxMindDbParameter;

public class StatePlace {
  private final String code;
  private final Place name;

  @MaxMindDbConstructor
  public StatePlace(
          @MaxMindDbParameter(name = "code") String code,
          @MaxMindDbParameter(name = "name") Place name
  ) {
    this.code = code;
    this.name = name;
  }

  public String getCode() { return code; }
  public Place getName() { return name; }
}
