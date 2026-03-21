package nl.markpost.weather.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.markpost.weather.api.v1.controller.WeatherApi;
import nl.markpost.weather.api.v1.model.WeatherResponse;
import nl.markpost.weather.mapper.WeatherModelMapper;
import nl.markpost.weather.model.Weather;
import nl.markpost.weather.service.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for weather retrieval API.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class WeatherController implements WeatherApi {

  private final WeatherService weatherService;
  private final WeatherModelMapper weatherModelMapper;

  /**
   * Retrieves weather data for the given coordinates.
   *
   * @param latitude  the latitude
   * @param longitude the longitude
   * @param language  BCP-47 language code for city names (en, nl, de, fr); optional
   * @return ResponseEntity with weather forecast
   */
  @Override
  public ResponseEntity<WeatherResponse> getWeather(Double latitude,
      Double longitude, String language) {
    log.info("Receive weather data at latitude: {}, longitude: {}", latitude, longitude);
    Weather weather = weatherService.getWeather(latitude, longitude, language);
    return ResponseEntity.ok(weatherModelMapper.from(weather));
  }

}
