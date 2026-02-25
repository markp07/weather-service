package nl.markpost.weather.model;

/**
 * Weather alarm level based on weather conditions.
 * Represents European-style weather warning levels (comparable to Dutch KNMI code system).
 */
public enum WeatherAlarm {

  GREEN,
  YELLOW,
  ORANGE,
  RED;

  /**
   * Determines the alarm level from a given weather code.
   *
   * @param weatherCode the weather code
   * @return the corresponding alarm level
   */
  public static WeatherAlarm fromWeatherCode(WeatherCode weatherCode) {
    if (weatherCode == null) {
      return GREEN;
    }
    return switch (weatherCode) {
      case THUNDERSTORM_SLIGHT_HAIL, THUNDERSTORM_HEAVY_HAIL -> RED;
      case RAIN_HEAVY, FREEZING_DRIZZLE_DENSE, FREEZING_RAIN_LIGHT, FREEZING_RAIN_HEAVY,
           SNOW_MODERATE, SNOW_HEAVY, SNOW_GRAINS, RAIN_SHOWERS_VIOLENT,
           SNOW_SHOWERS_HEAVY, THUNDERSTORM_SLIGHT_MODERATE -> ORANGE;
      case FOG, DEPOSITING_RIME_FOG, DRIZZLE_MODERATE, DRIZZLE_DENSE, RAIN_SLIGHT,
           RAIN_MODERATE, SNOW_SLIGHT, RAIN_SHOWERS_SLIGHT, RAIN_SHOWERS_MODERATE,
           SNOW_SHOWERS_SLIGHT, FREEZING_DRIZZLE_LIGHT -> YELLOW;
      default -> GREEN;
    };
  }

}
