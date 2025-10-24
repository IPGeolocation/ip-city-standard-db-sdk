package io.ipgeolocation.models;

import com.maxmind.db.MaxMindDbConstructor;
public class State {
  private final String code;
  private final String name;

  @MaxMindDbConstructor
  public State(String code, String name) {
    this.code = code;
    this.name = name;
  }

  public String getCode() { return code; }
  public String getName() { return name; }

  @Override
  public String toString() {
    return "State{" +
            "code='" + code + '\'' +
            ", name='" + name + '\'' +
            '}';
  }
}
