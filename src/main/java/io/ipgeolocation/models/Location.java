package io.ipgeolocation.models;


public class Location {
  private final City city;
  private final Coordinates coordinates;
  private final Country country;
  private final District district;
  private final String geonameId;
  private final State state;
  private final String zipcode;

  public Location(
          City city,
          Coordinates coordinates,
          Country country,
          District district,
          String geonameId,
          State state,
          String zipcode
  ) {
    this.city = city;
    this.coordinates = coordinates;
    this.country = country;
    this.district = district;
    this.geonameId = geonameId;
    this.state = state;
    this.zipcode = zipcode;
  }

  public City getCity() {
    return city;
  }

  public Coordinates getCoordinates() {
    return coordinates;
  }

  public Country getCountry() {
    return country;
  }

  public District getDistrict() {
    return district;
  }

  public String getGeonameId() {
    return geonameId;
  }

  public State getState() {
    return state;
  }

  public String getZipcode() {
    return zipcode;
  }

  @Override
  public String toString() {
    return "Location{" +
            "city=" + city +
            ", coordinates=" + coordinates +
            ", country=" + country +
            ", district=" + district +
            ", geonameId='" + geonameId + '\'' +
            ", state=" + state +
            ", zipcode='" + zipcode + '\'' +
            '}';
  }
}
