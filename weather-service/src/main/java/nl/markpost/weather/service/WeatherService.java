package nl.markpost.weather.service;

import lombok.RequiredArgsConstructor;
import nl.markpost.weather.client.OpenMeteoClient;
import nl.markpost.weather.client.ReverseGeocodeClient;
import nl.markpost.weather.mapper.WeatherMapper;
import nl.markpost.weather.model.ReverseGeocodeResponse;
import nl.markpost.weather.model.Weather;
import nl.markpost.weather.model.WeatherResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
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

  /**
   * Self-reference to ensure @Cacheable proxy is invoked for internal calls.
   */
  @Lazy
  @Autowired
  private WeatherService self;

  /**
   * Retrieves and maps weather data for the given coordinates.
   */
  public Weather getWeather(double latitude, double longitude) {
    WeatherResponse dailyWeatherResponse = self.getWeatherDaily(latitude, longitude);
    WeatherResponse hourlyWeatherResponse = self.getWeatherHourly(latitude, longitude);
    ReverseGeocodeResponse reverseGeocodeResponse = self.getLocation(latitude, longitude);
    if (dailyWeatherResponse != null && hourlyWeatherResponse != null) {
      hourlyWeatherResponse.setDaily(dailyWeatherResponse.getDaily());
    }
    return weatherMapper.toWeather(hourlyWeatherResponse, reverseGeocodeResponse);
  }

  /**
   * Retrieves daily weather data for the given coordinates, cached for 24h or until midnight.
   */
  @Cacheable(value = "weatherDaily", key = "#latitude + '-' + #longitude")
  public WeatherResponse getWeatherDaily(double latitude, double longitude) {
    return openMeteoClient.getWeatherDaily(latitude, longitude);
  }

  /**
   * Retrieves hourly weather data for the given coordinates, cached for 1 hour.
   */
  @Cacheable(value = "weatherHourly", key = "#latitude + '-' + #longitude")
  public WeatherResponse getWeatherHourly(double latitude, double longitude) {
    return openMeteoClient.getWeatherHourly(latitude, longitude);
  }

  /**
   * Retrieves the location name for the given coordinates.
   */
  @Cacheable(value = "location", key = "#latitude + '-' + #longitude")
  public ReverseGeocodeResponse getLocation(double latitude, double longitude) {
    return reverseGeocodeClient.getLocation(latitude, longitude);
  }

}
