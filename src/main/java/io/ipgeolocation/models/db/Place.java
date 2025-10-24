package io.ipgeolocation.models.db;

import com.maxmind.db.MaxMindDbParameter;

import com.maxmind.db.MaxMindDbConstructor;
import java.util.Objects;

import static com.google.common.base.Strings.nullToEmpty;


public class Place {
//  private final Integer id;

  private final String nameEnglish;  // en
  private final String nameGerman;   // de
  private final String nameRussian;  // ru
  private final String nameJapanese; // ja
  private final String nameFrench;   // fr
  private final String nameChinese;  // zh
  private final String nameSpanish;  // es
  private final String nameCzech;    // cs
  private final String nameItalian;  // it
  private final String namePersian;  // fa
  private final String nameKorean;   // ko
  private final String namePortuguese; // pt

  public Place(Integer id,
               String nameEnglish, String nameGerman, String nameRussian, String nameJapanese,
               String nameFrench, String nameChinese, String nameSpanish, String nameCzech,
               String nameItalian, String namePersian, String nameKorean, String namePortuguese) {

    if (Objects.isNull(id)) {
      throw new IllegalArgumentException("'id' must not be null.");
    }
//    this.id = id;

    this.nameEnglish     = dashToEmpty(nameEnglish);
    this.nameGerman      = nullToEmpty(nameGerman);
    this.nameRussian     = nullToEmpty(nameRussian);
    this.nameJapanese    = nullToEmpty(nameJapanese);
    this.nameFrench      = nullToEmpty(nameFrench);
    this.nameChinese     = nullToEmpty(nameChinese);
    this.nameSpanish     = nullToEmpty(nameSpanish);
    this.nameCzech       = nullToEmpty(nameCzech);
    this.nameItalian     = nullToEmpty(nameItalian);
    this.namePersian     = nullToEmpty(namePersian);
    this.nameKorean      = nullToEmpty(nameKorean);
    this.namePortuguese  = nullToEmpty(namePortuguese);
  }

  @MaxMindDbConstructor
  public Place(
          @MaxMindDbParameter(name = "en") String nameEnglish,
          @MaxMindDbParameter(name = "de") String nameGerman,
          @MaxMindDbParameter(name = "ru") String nameRussian,
          @MaxMindDbParameter(name = "ja") String nameJapanese,
          @MaxMindDbParameter(name = "fr") String nameFrench,
          @MaxMindDbParameter(name = "zh") String nameChinese,
          @MaxMindDbParameter(name = "es") String nameSpanish,
          @MaxMindDbParameter(name = "cs") String nameCzech,
          @MaxMindDbParameter(name = "it") String nameItalian,
          @MaxMindDbParameter(name = "fa") String namePersian,
          @MaxMindDbParameter(name = "ko") String nameKorean,
          @MaxMindDbParameter(name = "pt") String namePortuguese
  ) {
    this(-1,
            nameEnglish != null ? nameEnglish : "-",
            nameGerman, nameRussian, nameJapanese,
            nameFrench, nameChinese, nameSpanish, nameCzech,
            nameItalian, namePersian, nameKorean, namePortuguese);
  }

  private static String dashToEmpty(String s) {
    String v = nullToEmpty(s);
    return v.equals("-") ? "" : v;
  }

  public String getName(String lang) {
    if (lang == null || lang.isBlank()) return nameEnglish;
    String k = lang.toLowerCase(java.util.Locale.ROOT);
    // Accept "cn" as an alias for Chinese (data uses "zh")
    if ("cn".equals(k)) k = "zh";
    int dash = k.indexOf('-'); // handle "en-GB" -> "en"
    if (dash > 0) k = k.substring(0, dash);

    return switch (k) {
      case "de" -> nameGerman;
      case "ru" -> nameRussian;
      case "ja" -> nameJapanese;
      case "fr" -> nameFrench;
      case "zh" -> nameChinese;
      case "es" -> nameSpanish;
      case "cs" -> nameCzech;
      case "it" -> nameItalian;
      case "fa" -> namePersian;
      case "ko" -> nameKorean;
      case "pt" -> namePortuguese;
      default -> nameEnglish;
    };
  }

//  public Integer getId() { return id; }
  public String getNameEnglish() { return nameEnglish; }
  public String getNameGerman() { return nameGerman; }
  public String getNameRussian() { return nameRussian; }
  public String getNameJapanese() { return nameJapanese; }
  public String getNameFrench() { return nameFrench; }
  public String getNameChinese() { return nameChinese; }
  public String getNameSpanish() { return nameSpanish; }
  public String getNameCzech() { return nameCzech; }
  public String getNameItalian() { return nameItalian; }
  public String getNamePersian() { return namePersian; }
  public String getNameKorean() { return nameKorean; }
  public String getNamePortuguese() { return namePortuguese; }

  @Override
  public String toString() {
    return "Place{" +
//            "id=" + id +
            ", en='" + nameEnglish + '\'' +
            ", de='" + nameGerman + '\'' +
            ", ru='" + nameRussian + '\'' +
            ", ja='" + nameJapanese + '\'' +
            ", fr='" + nameFrench + '\'' +
            ", zh='" + nameChinese + '\'' +
            ", es='" + nameSpanish + '\'' +
            ", cs='" + nameCzech + '\'' +
            ", it='" + nameItalian + '\'' +
            ", fa='" + namePersian + '\'' +
            ", ko='" + nameKorean + '\'' +
            ", pt='" + namePortuguese + '\'' +
            '}';
  }
}