package nl.markpost.weather.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import nl.markpost.weather.client.OpenMeteoClient;
import nl.markpost.weather.client.ReverseGeocodeClient;
import nl.markpost.weather.mapper.WeatherMapper;
import nl.markpost.weather.model.Daily;
import nl.markpost.weather.model.ReverseGeocodeResponse;
import nl.markpost.weather.model.Weather;
import nl.markpost.weather.model.WeatherAlarm;
import nl.markpost.weather.model.WeatherResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service for retrieving and mapping weather data from the Open-Meteo API.
 */
@Service
@RequiredArgsConstructor
public class WeatherService {

  private final OpenMeteoClient openMeteoClient;

  private final ReverseGeocodeClient reverseGeocodeClient;

  private final WeatherMapper weatherMapper;

  private final MeteoAlarmService meteoAlarmService;

  /**
   * Retrieves and maps weather data for the given coordinates, including official weather
   * alarms from MeteoAlarm where available.
   */
  public Weather getWeather(double latitude, double longitude) {
    WeatherResponse dailyWeatherResponse = getWeatherDaily(latitude, longitude);
    WeatherResponse hourlyWeatherResponse = getWeatherHourly(latitude, longitude);
    ReverseGeocodeResponse reverseGeocodeResponse = getLocation(latitude, longitude);
    if (dailyWeatherResponse != null && hourlyWeatherResponse != null) {
      hourlyWeatherResponse.setDaily(dailyWeatherResponse.getDaily());
    }
    Weather weather = weatherMapper.toWeather(hourlyWeatherResponse, reverseGeocodeResponse);
    applyAlarms(weather, reverseGeocodeResponse);
    return weather;
  }

  /**
   * Applies official weather alarms from MeteoAlarm to the mapped Weather object.
   * Sets the overall alarm, per-day alarms, and list of active warning details.
   */
  private void applyAlarms(Weather weather, ReverseGeocodeResponse location) {
    if (weather == null || location == null || location.getCountryCode() == null) {
      return;
    }
    String countryCode = location.getCountryCode();
    WeatherAlarm overallAlarm = meteoAlarmService.getHighestAlarm(countryCode);
    weather.setAlarm(overallAlarm);
    weather.setAlarmWarnings(meteoAlarmService.getActiveWarnings(countryCode));

    if (weather.getDaily() != null && !weather.getDaily().isEmpty()) {
      List<LocalDate> dates = weather.getDaily().stream()
          .map(d -> d.getTime() != null ? d.getTime().toLocalDate() : null)
          .collect(Collectors.toList());
      List<WeatherAlarm> dailyAlarms = meteoAlarmService.getDailyAlarms(countryCode, dates);
      List<Daily> dailyList = weather.getDaily();
      for (int i = 0; i < dailyList.size() && i < dailyAlarms.size(); i++) {
        dailyList.get(i).setAlarm(dailyAlarms.get(i));
      }
    }
  }

  /**
   * Retrieves daily weather data for the given coordinates, cached for 24h or until midnight.
   */
  @Cacheable(value = "weatherDaily", key = "#latitude + '-' + #longitude")
  private WeatherResponse getWeatherDaily(double latitude, double longitude) {
    return openMeteoClient.getWeatherDaily(latitude, longitude);
  }

  /**
   * Retrieves hourly weather data for the given coordinates, cached for 1 hour.
   */
  @Cacheable(value = "weatherHourly", key = "#latitude + '-' + #longitude")
  private WeatherResponse getWeatherHourly(double latitude, double longitude) {
    return openMeteoClient.getWeatherHourly(latitude, longitude);
  }

  /**
   * Retrieves the location name for the given coordinates.
   */
  @Cacheable(value = "location", key = "#latitude + '-' + #longitude")
  private ReverseGeocodeResponse getLocation(double latitude, double longitude) {
    return reverseGeocodeClient.getLocation(latitude, longitude);
  }

}
