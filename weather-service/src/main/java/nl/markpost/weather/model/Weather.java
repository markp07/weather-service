package nl.markpost.weather.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Weather implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private double latitude;

  private double longitude;

  private String location;

  private String timezone;

  private double elevation;

  private Current current;

  private List<Daily> daily;

  private List<Hourly> hourly;

  private WeatherAlarm alarm;

  private List<MeteoAlarmWarning> alarmWarnings;

}
