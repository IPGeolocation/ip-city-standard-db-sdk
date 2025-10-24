package io.ipgeolocation.models.db;

import com.maxmind.db.MaxMindDbConstructor;
import com.maxmind.db.MaxMindDbParameter;
import io.ipgeolocation.models.Coordinates;

public class LocationPlace {
  private final CityPlace cityPlace;
  private final Coordinates coordinates;
  private final CountryPlace country;
  private final DistrictPlace district;
  private final String geonameId;
  private final StatePlace state;
  private final String zipcode;
//  private final String timeZone;

  @MaxMindDbConstructor
  public LocationPlace(
          @MaxMindDbParameter(name = "city") CityPlace cityPlace,
          @MaxMindDbParameter(name = "coordinates") Coordinates coordinates,
          @MaxMindDbParameter(name = "country") CountryPlace country,
          @MaxMindDbParameter(name = "district") DistrictPlace district,
          @MaxMindDbParameter(name = "geoname_id") String geonameId,
          @MaxMindDbParameter(name = "state") StatePlace state,
          @MaxMindDbParameter(name = "zipcode") String zipcode
//          @MaxMindDbParameter(name = "time_zone") String timeZone
  ) {
    this.cityPlace = cityPlace;
    this.coordinates = coordinates;
    this.country = country;
    this.district = district;
    this.geonameId = geonameId;
    this.state = state;
    this.zipcode = zipcode;
//    this.timeZone = timeZone;
  }

  public CityPlace getCity() { return cityPlace; }
  public Coordinates getCoordinates() { return coordinates; }
  public CountryPlace getCountry() { return country; }
  public DistrictPlace getDistrict() { return district; }
  public String getGeonameId() { return geonameId; }
  public StatePlace getState() { return state; }
  public String getZipcode() { return zipcode; }
//  public String getTimeZone() { return timeZone; }
}
