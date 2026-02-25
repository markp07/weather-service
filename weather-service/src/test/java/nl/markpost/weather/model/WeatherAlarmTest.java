package nl.markpost.weather.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WeatherAlarmTest {

  @Test
  @DisplayName("Should return GREEN for clear and calm weather codes")
  void fromWeatherCode_green() {
    assertEquals(WeatherAlarm.GREEN, WeatherAlarm.fromWeatherCode(WeatherCode.CLEAR_SKY));
    assertEquals(WeatherAlarm.GREEN, WeatherAlarm.fromWeatherCode(WeatherCode.MAINLY_CLEAR));
    assertEquals(WeatherAlarm.GREEN, WeatherAlarm.fromWeatherCode(WeatherCode.PARTLY_CLOUDY));
    assertEquals(WeatherAlarm.GREEN, WeatherAlarm.fromWeatherCode(WeatherCode.OVERCAST));
    assertEquals(WeatherAlarm.GREEN, WeatherAlarm.fromWeatherCode(WeatherCode.DRIZZLE_LIGHT));
  }

  @Test
  @DisplayName("Should return YELLOW for moderate adverse weather codes")
  void fromWeatherCode_yellow() {
    assertEquals(WeatherAlarm.YELLOW, WeatherAlarm.fromWeatherCode(WeatherCode.FOG));
    assertEquals(WeatherAlarm.YELLOW, WeatherAlarm.fromWeatherCode(WeatherCode.DEPOSITING_RIME_FOG));
    assertEquals(WeatherAlarm.YELLOW, WeatherAlarm.fromWeatherCode(WeatherCode.DRIZZLE_MODERATE));
    assertEquals(WeatherAlarm.YELLOW, WeatherAlarm.fromWeatherCode(WeatherCode.DRIZZLE_DENSE));
    assertEquals(WeatherAlarm.YELLOW, WeatherAlarm.fromWeatherCode(WeatherCode.RAIN_SLIGHT));
    assertEquals(WeatherAlarm.YELLOW, WeatherAlarm.fromWeatherCode(WeatherCode.RAIN_MODERATE));
    assertEquals(WeatherAlarm.YELLOW, WeatherAlarm.fromWeatherCode(WeatherCode.SNOW_SLIGHT));
    assertEquals(WeatherAlarm.YELLOW, WeatherAlarm.fromWeatherCode(WeatherCode.RAIN_SHOWERS_SLIGHT));
    assertEquals(WeatherAlarm.YELLOW, WeatherAlarm.fromWeatherCode(WeatherCode.RAIN_SHOWERS_MODERATE));
    assertEquals(WeatherAlarm.YELLOW, WeatherAlarm.fromWeatherCode(WeatherCode.SNOW_SHOWERS_SLIGHT));
    assertEquals(WeatherAlarm.YELLOW, WeatherAlarm.fromWeatherCode(WeatherCode.FREEZING_DRIZZLE_LIGHT));
  }

  @Test
  @DisplayName("Should return ORANGE for severe weather codes")
  void fromWeatherCode_orange() {
    assertEquals(WeatherAlarm.ORANGE, WeatherAlarm.fromWeatherCode(WeatherCode.RAIN_HEAVY));
    assertEquals(WeatherAlarm.ORANGE, WeatherAlarm.fromWeatherCode(WeatherCode.FREEZING_DRIZZLE_DENSE));
    assertEquals(WeatherAlarm.ORANGE, WeatherAlarm.fromWeatherCode(WeatherCode.FREEZING_RAIN_LIGHT));
    assertEquals(WeatherAlarm.ORANGE, WeatherAlarm.fromWeatherCode(WeatherCode.FREEZING_RAIN_HEAVY));
    assertEquals(WeatherAlarm.ORANGE, WeatherAlarm.fromWeatherCode(WeatherCode.SNOW_MODERATE));
    assertEquals(WeatherAlarm.ORANGE, WeatherAlarm.fromWeatherCode(WeatherCode.SNOW_HEAVY));
    assertEquals(WeatherAlarm.ORANGE, WeatherAlarm.fromWeatherCode(WeatherCode.SNOW_GRAINS));
    assertEquals(WeatherAlarm.ORANGE, WeatherAlarm.fromWeatherCode(WeatherCode.RAIN_SHOWERS_VIOLENT));
    assertEquals(WeatherAlarm.ORANGE, WeatherAlarm.fromWeatherCode(WeatherCode.SNOW_SHOWERS_HEAVY));
    assertEquals(WeatherAlarm.ORANGE, WeatherAlarm.fromWeatherCode(WeatherCode.THUNDERSTORM_SLIGHT_MODERATE));
  }

  @Test
  @DisplayName("Should return RED for extreme weather codes")
  void fromWeatherCode_red() {
    assertEquals(WeatherAlarm.RED, WeatherAlarm.fromWeatherCode(WeatherCode.THUNDERSTORM_SLIGHT_HAIL));
    assertEquals(WeatherAlarm.RED, WeatherAlarm.fromWeatherCode(WeatherCode.THUNDERSTORM_HEAVY_HAIL));
  }

  @Test
  @DisplayName("Should return GREEN for null weather code")
  void fromWeatherCode_null() {
    assertEquals(WeatherAlarm.GREEN, WeatherAlarm.fromWeatherCode(null));
  }

}
