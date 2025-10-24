package io.ipgeolocation.models.db;


import com.maxmind.db.MaxMindDbConstructor;
import com.maxmind.db.MaxMindDbParameter;

public class CurrencyPlace {
  private final String code;
  private final Place name;
  private final String symbol;

  @MaxMindDbConstructor
  public CurrencyPlace(
          @MaxMindDbParameter(name = "code") String code,
          @MaxMindDbParameter(name = "name") Place name,
          @MaxMindDbParameter(name = "symbol") String symbol
  ) {
    this.code = code;
    this.name = name;
    this.symbol = symbol;
  }

  public String getCode() { return code; }

  public String getName() { return name != null ? name.getNameEnglish() : ""; }

  public Place getNamePlace() { return name; }

  public String getSymbol() { return symbol; }

  @Override
  public String toString() {
    return "Currency{" +
            "code='" + code + '\'' +
            ", name_en='" + getName() + '\'' +
            ", symbol='" + symbol + '\'' +
            '}';
  }
}