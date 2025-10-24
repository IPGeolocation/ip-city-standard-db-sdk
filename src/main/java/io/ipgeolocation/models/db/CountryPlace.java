package io.ipgeolocation.models.db;

import com.maxmind.db.MaxMindDbConstructor;
import com.maxmind.db.MaxMindDbParameter;
import io.ipgeolocation.models.CountryMetadata;

public class CountryPlace {
  private final Place capital;
  private final String code2;
  private final String code3;
  private final String codeIoc;
  private final ContinentPlace continent;
  private final CurrencyPlace currency;
  private final CountryMetadata metadata;
  private final Place name;
  private final Place nameOfficial;

  @MaxMindDbConstructor
  public CountryPlace(
          @MaxMindDbParameter(name = "capital") Place capital,
          @MaxMindDbParameter(name = "code2") String code2,
          @MaxMindDbParameter(name = "code3") String code3,
          @MaxMindDbParameter(name = "code_ioc") String codeIoc,
          @MaxMindDbParameter(name = "continent") ContinentPlace continent,
          @MaxMindDbParameter(name = "currency") CurrencyPlace currency,
          @MaxMindDbParameter(name = "metadata") CountryMetadata metadata,
          @MaxMindDbParameter(name = "name") Place name,
          @MaxMindDbParameter(name = "name_official") Place nameOfficial
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

  public Place getCapital() { return capital; }
  public String getCode2() { return code2; }
  public String getCode3() { return code3; }
  public String getCodeIoc() { return codeIoc; }
  public ContinentPlace getContinent() { return continent; }
  public CurrencyPlace getCurrency() { return currency; }
  public CountryMetadata getMetadata() { return metadata; }
  public Place getName() { return name; }
  public Place getNameOfficial() { return nameOfficial; }
}