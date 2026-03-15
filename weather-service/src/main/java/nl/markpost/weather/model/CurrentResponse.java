package nl.markpost.weather.model;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing the current weather data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentResponse implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String time;

  private Integer interval;

  private Integer weather_code;

  private Double temperature_2m;

  private Integer wind_speed_10m;

  private Integer wind_direction_10m;

}

