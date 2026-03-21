package nl.markpost.weather.service;

import lombok.RequiredArgsConstructor;
import nl.markpost.weather.client.OpenMeteoClient;
import nl.markpost.weather.client.ReverseGeocodeClient;
import nl.markpost.weather.mapper.WeatherMapper;
import nl.markpost.weather.model.ReverseGeocodeResponse;
import nl.markpost.weather.model.Weather;
import nl.markpost.weather.model.WeatherResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${reverse-geocode.language:en}")
  private String defaultLanguage;

  /**
   * Self-reference to ensure @Cacheable proxy is invoked for internal calls.
   */
  @Lazy
  @Autowired
  private WeatherService self;

  /**
   * Retrieves and maps weather data for the given coordinates.
   *
   * @param language BCP-47 language code for city names; falls back to configured default when null
   */
  public Weather getWeather(double latitude, double longitude, String language) {
    String lang = resolveLanguage(language);
    WeatherResponse dailyWeatherResponse = self.getWeatherDaily(latitude, longitude);
    WeatherResponse hourlyWeatherResponse = self.getWeatherHourly(latitude, longitude);
    ReverseGeocodeResponse reverseGeocodeResponse = self.getLocation(latitude, longitude, lang);
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
   * Retrieves the location name for the given coordinates and language, cached per coordinate+language pair.
   */
  @Cacheable(value = "location", key = "#latitude + '-' + #longitude + '-' + #language")
  public ReverseGeocodeResponse getLocation(double latitude, double longitude, String language) {
    return reverseGeocodeClient.getLocation(latitude, longitude, language);
  }

  private String resolveLanguage(String language) {
    return (language != null) ? language : defaultLanguage;
  }

}
