package io.ipgeolocation.models;


public class Country {
  private final String capital;
  private final String code2;
  private final String code3;
  private final String codeIoc;
  private final Continent continent;
  private final Currency currency;
  private final CountryMetadata metadata;
  private final String name;
  private final String nameOfficial;

  public Country(
          String capital,
          String code2,
          String code3,
          String codeIoc,
          Continent continent,
          Currency currency,
          CountryMetadata metadata,
          String name,
          String nameOfficial
  ) {
    this.capital = capital;
    this.code2 = code2;
    this.code3 = code3;
    this.codeIoc = codeIoc;
    this.continent = continent;
    this.currency = currency;
    this.metadata = metadata;
    this.name = name;
    this.nameOfficial = nameOfficial;
  }

  public String getCapital() {
    return capital;
  }

  public String getCode2() {
    return code2;
  }

  public String getCode3() {
    return code3;
  }

  public String getCodeIoc() {
    return codeIoc;
  }

  public Continent getContinent() {
    return continent;
  }

  public Currency getCurrency() {
    return currency;
  }

  public CountryMetadata getMetadata() {
    return metadata;
  }

  public String getName() {
    return name;
  }

  public String getNameOfficial() {
    return nameOfficial;
  }

  @Override
  public String toString() {
    return "Country{" +
            "capital='" + capital + '\'' +
            ", code2='" + code2 + '\'' +
            ", code3='" + code3 + '\'' +
            ", codeIoc='" + codeIoc + '\'' +
            ", continent=" + continent +
            ", currency=" + currency +
            ", metadata=" + metadata +
            ", name='" + name + '\'' +
            ", nameOfficial='" + nameOfficial + '\'' +
            '}';
  }
}