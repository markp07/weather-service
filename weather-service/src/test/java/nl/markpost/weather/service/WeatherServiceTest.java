package nl.markpost.weather.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import nl.markpost.weather.client.OpenMeteoClient;
import nl.markpost.weather.client.ReverseGeocodeClient;
import nl.markpost.weather.mapper.WeatherMapper;
import nl.markpost.weather.model.Daily;
import nl.markpost.weather.model.ReverseGeocodeResponse;
import nl.markpost.weather.model.Weather;
import nl.markpost.weather.model.WeatherAlarm;
import nl.markpost.weather.model.WeatherCode;
import nl.markpost.weather.model.WeatherResponse;
import nl.markpost.weather.model.WindDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

  @Mock
  private OpenMeteoClient openMeteoClient;

  @Mock
  private WeatherMapper weatherMapper;

  @Mock
  private ReverseGeocodeClient reverseGeocodeClient;

  @Mock
  private MeteoAlarmService meteoAlarmService;

  @InjectMocks
  private WeatherService weatherService;

  @BeforeEach
  void setUp() {
    // In unit tests there is no Spring proxy, so wire self to the instance itself so
    // getWeather() can call through to the (un-cached) public methods.
    ReflectionTestUtils.setField(weatherService, "self", weatherService);
  }

  @Test
  @DisplayName("Should call OpenMeteoClient, ReverseGeocodeClient, and WeatherMapper and return mapped Weather")
  void getWeather_success() {
    double latitude = 52.0;
    double longitude = 4.0;
    WeatherResponse weatherResponse = mock(WeatherResponse.class);
    ReverseGeocodeResponse reverseGeocodeResponse = mock(ReverseGeocodeResponse.class);
    Weather expectedWeather = mock(Weather.class);

    when(openMeteoClient.getWeatherDaily(latitude, longitude)).thenReturn(weatherResponse);
    when(openMeteoClient.getWeatherHourly(latitude, longitude)).thenReturn(weatherResponse);
    when(reverseGeocodeClient.getLocation(latitude, longitude)).thenReturn(reverseGeocodeResponse);
    when(weatherMapper.toWeather(weatherResponse, reverseGeocodeResponse)).thenReturn(
        expectedWeather);

    Weather result = weatherService.getWeather(latitude, longitude);

    assertSame(expectedWeather, result);
    verify(openMeteoClient).getWeatherDaily(latitude, longitude);
    verify(openMeteoClient).getWeatherHourly(latitude, longitude);
    verify(reverseGeocodeClient).getLocation(latitude, longitude);
    verify(weatherMapper).toWeather(weatherResponse, reverseGeocodeResponse);
  }

  @Test
  @DisplayName("Should apply MeteoAlarm alarm when country code is present")
  void getWeather_appliesAlarm() {
    double latitude = 52.0;
    double longitude = 4.0;
    WeatherResponse weatherResponse = mock(WeatherResponse.class);
    ReverseGeocodeResponse location = new ReverseGeocodeResponse();
    location.setCountryCode("NL");
    Daily daily = new Daily(LocalDateTime.now(), WeatherCode.CLEAR_SKY, 10.0, 20.0, 0.0, 0, 10,
        WindDirection.N, null, null, null);
    Weather weather = new Weather(52.0, 4.0, "Amsterdam", "Europe/Amsterdam", 5.0, null,
        List.of(daily), List.of(), null, null);

    when(openMeteoClient.getWeatherDaily(latitude, longitude)).thenReturn(weatherResponse);
    when(openMeteoClient.getWeatherHourly(latitude, longitude)).thenReturn(weatherResponse);
    when(reverseGeocodeClient.getLocation(latitude, longitude)).thenReturn(location);
    when(weatherMapper.toWeather(weatherResponse, location)).thenReturn(weather);
    when(meteoAlarmService.getHighestAlarm(eq("NL"), anyDouble(), anyDouble(), isNull())).thenReturn(WeatherAlarm.YELLOW);
    when(meteoAlarmService.getActiveWarnings(eq("NL"), anyDouble(), anyDouble(), isNull())).thenReturn(Collections.emptyList());
    when(meteoAlarmService.getDailyAlarms(anyString(), anyDouble(), anyDouble(), any(), any())).thenReturn(
        Collections.singletonList(WeatherAlarm.YELLOW));

    Weather result = weatherService.getWeather(latitude, longitude);

    assertEquals(WeatherAlarm.YELLOW, result.getAlarm());
    assertEquals(WeatherAlarm.YELLOW, result.getDaily().get(0).getAlarm());
    verify(meteoAlarmService).getHighestAlarm(eq("NL"), anyDouble(), anyDouble(), isNull());
  }

  @Test
  @DisplayName("Should skip alarm when reverseGeocodeResponse is null")
  void getWeather_nullReverseGeocodeResponse_noAlarmFetch() {
    double latitude = 52.0;
    double longitude = 4.0;
    WeatherResponse dailyWeatherResponse = mock(WeatherResponse.class);
    WeatherResponse hourlyWeatherResponse = mock(WeatherResponse.class);
    Weather expectedWeather = mock(Weather.class);
    when(openMeteoClient.getWeatherDaily(latitude, longitude)).thenReturn(dailyWeatherResponse);
    when(openMeteoClient.getWeatherHourly(latitude, longitude)).thenReturn(hourlyWeatherResponse);
    when(reverseGeocodeClient.getLocation(latitude, longitude)).thenReturn(null);
    when(weatherMapper.toWeather(hourlyWeatherResponse, null)).thenReturn(expectedWeather);

    Weather result = weatherService.getWeather(latitude, longitude);

    assertSame(expectedWeather, result);
    verify(meteoAlarmService, never()).getHighestAlarm(anyString(), anyDouble(), anyDouble(), any());
  }

  @Test
  @DisplayName("Should handle null WeatherResponse from client")
  void getWeather_nullResponse() {
    double latitude = 52.0;
    double longitude = 4.0;
    when(openMeteoClient.getWeatherDaily(latitude, longitude)).thenReturn(null);
    when(openMeteoClient.getWeatherHourly(latitude, longitude)).thenReturn(null);
    when(reverseGeocodeClient.getLocation(latitude, longitude)).thenReturn(null);
    when(weatherMapper.toWeather(null, null)).thenReturn(null);
    Weather result = weatherService.getWeather(latitude, longitude);
    assertNull(result);
    verify(openMeteoClient).getWeatherDaily(latitude, longitude);
    verify(openMeteoClient).getWeatherHourly(latitude, longitude);
    verify(reverseGeocodeClient).getLocation(latitude, longitude);
    verify(weatherMapper).toWeather(null, null);
  }

  @Test
  @DisplayName("Should handle null dailyWeatherResponse")
  void getWeather_nullDailyWeatherResponse() {
    double latitude = 52.0;
    double longitude = 4.0;
    WeatherResponse hourlyWeatherResponse = mock(WeatherResponse.class);
    ReverseGeocodeResponse reverseGeocodeResponse = mock(ReverseGeocodeResponse.class);
    Weather expectedWeather = mock(Weather.class);
    when(openMeteoClient.getWeatherDaily(latitude, longitude)).thenReturn(null);
    when(openMeteoClient.getWeatherHourly(latitude, longitude)).thenReturn(hourlyWeatherResponse);
    when(reverseGeocodeClient.getLocation(latitude, longitude)).thenReturn(reverseGeocodeResponse);
    when(weatherMapper.toWeather(hourlyWeatherResponse, reverseGeocodeResponse)).thenReturn(
        expectedWeather);
    Weather result = weatherService.getWeather(latitude, longitude);
    assertSame(expectedWeather, result);
    verify(openMeteoClient).getWeatherDaily(latitude, longitude);
    verify(openMeteoClient).getWeatherHourly(latitude, longitude);
    verify(reverseGeocodeClient).getLocation(latitude, longitude);
    verify(weatherMapper).toWeather(hourlyWeatherResponse, reverseGeocodeResponse);
  }

  @Test
  @DisplayName("Should handle null hourlyWeatherResponse")
  void getWeather_nullHourlyWeatherResponse() {
    double latitude = 52.0;
    double longitude = 4.0;
    WeatherResponse dailyWeatherResponse = mock(WeatherResponse.class);
    ReverseGeocodeResponse reverseGeocodeResponse = mock(ReverseGeocodeResponse.class);
    Weather expectedWeather = mock(Weather.class);
    when(openMeteoClient.getWeatherDaily(latitude, longitude)).thenReturn(dailyWeatherResponse);
    when(openMeteoClient.getWeatherHourly(latitude, longitude)).thenReturn(null);
    when(reverseGeocodeClient.getLocation(latitude, longitude)).thenReturn(reverseGeocodeResponse);
    when(weatherMapper.toWeather(null, reverseGeocodeResponse)).thenReturn(expectedWeather);
    Weather result = weatherService.getWeather(latitude, longitude);
    assertSame(expectedWeather, result);
    verify(openMeteoClient).getWeatherDaily(latitude, longitude);
    verify(openMeteoClient).getWeatherHourly(latitude, longitude);
    verify(reverseGeocodeClient).getLocation(latitude, longitude);
    verify(weatherMapper).toWeather(null, reverseGeocodeResponse);
  }

  @Test
  @DisplayName("Should handle weatherMapper returning null")
  void getWeather_weatherMapperReturnsNull() {
    double latitude = 52.0;
    double longitude = 4.0;
    WeatherResponse dailyWeatherResponse = mock(WeatherResponse.class);
    WeatherResponse hourlyWeatherResponse = mock(WeatherResponse.class);
    ReverseGeocodeResponse reverseGeocodeResponse = mock(ReverseGeocodeResponse.class);
    when(openMeteoClient.getWeatherDaily(latitude, longitude)).thenReturn(dailyWeatherResponse);
    when(openMeteoClient.getWeatherHourly(latitude, longitude)).thenReturn(hourlyWeatherResponse);
    when(reverseGeocodeClient.getLocation(latitude, longitude)).thenReturn(reverseGeocodeResponse);
    when(weatherMapper.toWeather(hourlyWeatherResponse, reverseGeocodeResponse)).thenReturn(null);
    Weather result = weatherService.getWeather(latitude, longitude);
    assertNull(result);
    verify(openMeteoClient).getWeatherDaily(latitude, longitude);
    verify(openMeteoClient).getWeatherHourly(latitude, longitude);
    verify(reverseGeocodeClient).getLocation(latitude, longitude);
    verify(weatherMapper).toWeather(hourlyWeatherResponse, reverseGeocodeResponse);
  }

}
