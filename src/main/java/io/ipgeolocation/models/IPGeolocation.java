package io.ipgeolocation.models;

import io.ipgeolocation.models.db.*;

import java.util.Locale;

public class IPGeolocation {

  private final String connectionType;
  private final Location location;
  private final String timeZone;

  public IPGeolocation(
          String connectionType,
          Location location,
          String timeZone
  ) {
    this.connectionType = connectionType;
    this.location = location;
    this.timeZone = timeZone;
  }

  public String getConnectionType() {
    return connectionType;
  }

  public Location getLocation() {
    return location;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public static IPGeolocation from(IPCityPlace source, String lang) {
    String normalizedLanguage = normalizeLang(lang);

    // ── Location subtree
    LocationPlace locationPlace = source.getLocation();

    String cityName =
            place(locationPlace != null && locationPlace.getCity() != null
                    ? locationPlace.getCity().getName() : null, normalizedLanguage);

    String districtName =
            place(locationPlace != null && locationPlace.getDistrict() != null
                    ? locationPlace.getDistrict().getName() : null, normalizedLanguage);

    String stateCode =
            (locationPlace != null && locationPlace.getState() != null)
                    ? locationPlace.getState().getCode() : null;

    String stateName =
            place(locationPlace != null && locationPlace.getState() != null
                    ? locationPlace.getState().getName() : null, normalizedLanguage);

    Coordinates coordinates =
            (locationPlace != null) ? locationPlace.getCoordinates() : null;


    // ── Country subtree
    CountryPlace countryPlace =
            (locationPlace != null) ? locationPlace.getCountry() : null;

    String countryCapitalName =
            place((countryPlace != null) ? countryPlace.getCapital() : null, normalizedLanguage);

    ContinentPlace continentPlace =
            (countryPlace != null) ? countryPlace.getContinent() : null;

    Continent continent = new Continent(
            (continentPlace != null) ? continentPlace.getCode() : null,
            place((continentPlace != null) ? continentPlace.getName() : null, normalizedLanguage)
    );

    CurrencyPlace currencyPlace =
            (countryPlace != null) ? countryPlace.getCurrency() : null;

    String currencyName = null;
    if (currencyPlace != null) {
      Place currencyNamePlace = currencyPlace.getNamePlace();
      currencyName = (currencyNamePlace != null)
              ? currencyNamePlace.getName(normalizedLanguage)
              : currencyPlace.getName();
    }

    Currency currency = new Currency(
            (currencyPlace != null) ? currencyPlace.getCode() : null,
            currencyName,
            (currencyPlace != null) ? currencyPlace.getSymbol() : null
    );

    CountryMetadata sourceCountryMetadata =
            (countryPlace != null) ? countryPlace.getMetadata() : null;

    CountryMetadata countryMetadata = new CountryMetadata(
            (sourceCountryMetadata != null) ? sourceCountryMetadata.getCallingCode() : null,
            (sourceCountryMetadata != null) ? sourceCountryMetadata.getLanguages() : null,
            (sourceCountryMetadata != null) ? sourceCountryMetadata.getTld() : null
    );

    Country country = new Country(
            countryCapitalName,
            (countryPlace != null) ? countryPlace.getCode2() : null,
            (countryPlace != null) ? countryPlace.getCode3() : null,
            (countryPlace != null) ? countryPlace.getCodeIoc() : null,
            continent,
            currency,
            countryMetadata,
            place((countryPlace != null) ? countryPlace.getName() : null, normalizedLanguage),
            place((countryPlace != null) ? countryPlace.getNameOfficial() : null, normalizedLanguage)
    );

    Location location = new Location(
            new City(cityName),
            coordinates,
            country,
            new District(districtName),
            (locationPlace != null) ? locationPlace.getGeonameId() : null,
            new State(stateCode, stateName),
            (locationPlace != null) ? locationPlace.getZipcode() : null
    );

    return new IPGeolocation(source.getConnectionType(), location, source.getTimeZone());
  }

  private static String place(Place p, String lang) {
    return p == null ? "" : p.getName(lang);
  }

  private static String normalizeLang(String lang) {
    if (lang == null || lang.isBlank()) return "en";
    String k = lang.toLowerCase(Locale.ROOT);
    if ("cn".equals(k)) return "zh";          // accept "cn" alias
    int dash = k.indexOf('-');                // en-GB => en
    return dash > 0 ? k.substring(0, dash) : k;
  }

  @Override
  public String toString() {
    return "IPGeolocation{" +
            "connectionType='" + connectionType + '\'' +
            ", location=" + location +
            ", timeZone='" + timeZone + '\'' +
            '}';
  }
}