package nl.markpost.weather.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing the hourly weather data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HourlyResponse implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private List<String> time;

  private List<Integer> weather_code;

  private List<Double> temperature_2m;

  private List<Integer> precipitation_probability;

  private List<Double> precipitation;

  private List<Integer> wind_speed_10m;

  private List<Integer> wind_direction_10m;
}

