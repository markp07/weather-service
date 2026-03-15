package nl.markpost.weather.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WeatherAlarmTest {

  @Test
  @DisplayName("Should return GREEN for null or unrecognised awareness level")
  void fromAwarenessLevel_green() {
    assertEquals(WeatherAlarm.GREEN, WeatherAlarm.fromAwarenessLevel(null));
    assertEquals(WeatherAlarm.GREEN, WeatherAlarm.fromAwarenessLevel("1; green; No warning"));
    assertEquals(WeatherAlarm.GREEN, WeatherAlarm.fromAwarenessLevel("minor"));
    assertEquals(WeatherAlarm.GREEN, WeatherAlarm.fromAwarenessLevel("unknown"));
  }

  @Test
  @DisplayName("Should return YELLOW for CAP format level 2 or yellow/moderate keyword")
  void fromAwarenessLevel_yellow() {
    assertEquals(WeatherAlarm.YELLOW, WeatherAlarm.fromAwarenessLevel("2; yellow; Moderate"));
    assertEquals(WeatherAlarm.YELLOW, WeatherAlarm.fromAwarenessLevel("Yellow warning for Wind"));
    assertEquals(WeatherAlarm.YELLOW, WeatherAlarm.fromAwarenessLevel("YELLOW"));
    assertEquals(WeatherAlarm.YELLOW, WeatherAlarm.fromAwarenessLevel("Moderate wind warning"));
  }

  @Test
  @DisplayName("Should return ORANGE for CAP format level 3 or orange/severe keyword")
  void fromAwarenessLevel_orange() {
    assertEquals(WeatherAlarm.ORANGE, WeatherAlarm.fromAwarenessLevel("3; orange; Severe"));
    assertEquals(WeatherAlarm.ORANGE, WeatherAlarm.fromAwarenessLevel("Orange warning for Rain"));
    assertEquals(WeatherAlarm.ORANGE, WeatherAlarm.fromAwarenessLevel("ORANGE"));
    assertEquals(WeatherAlarm.ORANGE, WeatherAlarm.fromAwarenessLevel("Severe thunderstorm"));
  }

  @Test
  @DisplayName("Should return RED for CAP format level 4 or red/extreme keyword")
  void fromAwarenessLevel_red() {
    assertEquals(WeatherAlarm.RED, WeatherAlarm.fromAwarenessLevel("4; red; Extreme"));
    assertEquals(WeatherAlarm.RED, WeatherAlarm.fromAwarenessLevel("Red warning for Thunderstorm"));
    assertEquals(WeatherAlarm.RED, WeatherAlarm.fromAwarenessLevel("RED"));
    assertEquals(WeatherAlarm.RED, WeatherAlarm.fromAwarenessLevel("Extreme wind warning"));
  }

}
