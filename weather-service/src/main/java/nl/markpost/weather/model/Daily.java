package nl.markpost.weather.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Daily implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private LocalDateTime time;

  private WeatherCode weatherCode;

  private Double temperatureMin;

  private Double temperatureMax;

  private Double precipitation;

  private Integer precipitationProbabilityMax;

  private Integer windSpeed;

  private WindDirection windDirection;

  private LocalDateTime sunRise;

  private LocalDateTime sunSet;

}
