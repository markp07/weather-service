package nl.markpost.weather.model;

/**
 * Weather alarm level, comparable to the European/Dutch KNMI warning code system.
 * Alarm levels are sourced from MeteoAlarm (meteoalarm.org), the official European
 * weather warning aggregator backed by national meteorological services.
 */
public enum WeatherAlarm {

  GREEN,
  YELLOW,
  ORANGE,
  RED;

  /**
   * Derives the alarm level from a MeteoAlarm awareness_level string.
   * Handles both the CAP parameter format (e.g. "2; yellow; Moderate") and plain
   * color keywords (e.g. "yellow") as found in Atom entry titles.
   *
   * @param awarenessLevel awareness level string from MeteoAlarm
   * @return the corresponding WeatherAlarm level
   */
  public static WeatherAlarm fromAwarenessLevel(String awarenessLevel) {
    if (awarenessLevel == null) {
      return GREEN;
    }
    String lower = awarenessLevel.toLowerCase();
    // CAP format starts with level number: "4; red; Extreme", "3; orange; Severe", etc.
    // Also match plain color keywords used in entry titles.
    if (lower.startsWith("4") || lower.contains("red") || lower.contains("extreme")) {
      return RED;
    }
    if (lower.startsWith("3") || lower.contains("orange") || lower.contains("severe")) {
      return ORANGE;
    }
    if (lower.startsWith("2") || lower.contains("yellow") || lower.contains("moderate")) {
      return YELLOW;
    }
    return GREEN;
  }

}
