package io.ipgeolocation.models;

public class District {
  private final String name;

  public District( String name) {
    this.name = name;
  }

  public String getName() { return name; }

  @Override
  public String toString() {
    return "District{" +
            "name='" + name + '\'' +
            '}';
  }
}
