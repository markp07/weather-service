package nl.markpost.weather.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import nl.markpost.weather.model.ReverseGeocodeResponse;
import nl.markpost.weather.model.WeatherResponse;
import org.junit.jupiter.api.AfterAll;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Integration test that verifies cached model classes are properly {@link java.io.Serializable}.
 *
 * <p>Before the fix, Redis cache writes would throw
 * {@code SerializationException: DefaultSerializer requires a Serializable payload} because
 * {@code WeatherResponse}, {@code ReverseGeocodeResponse} and their nested types did not
 * implement {@code java.io.Serializable}. This test:
 * <ol>
 *   <li>Starts a full Spring context with WireMock stubs for all external HTTP APIs.</li>
 *   <li>Fetches weather data through {@link WeatherService}, which populates the in-memory cache.</li>
 *   <li>Retrieves the cached values and <em>serializes them with JDK {@link ObjectOutputStream}</em>,
 *       replicating exactly what {@code JdkSerializationRedisSerializer} does in production.
 *       Before the fix this step would throw {@code java.io.NotSerializableException}.</li>
 *   <li>Verifies a second call for the same coordinates is served from cache (no WireMock call).</li>
 * </ol>
 *
 * <p>Uses {@code spring.cache.type=simple} (ConcurrentMapCacheManager) so no Redis instance is
 * required; the serialization check is done explicitly via {@link ObjectOutputStream}.
 */
@SpringBootTest
@ActiveProfiles("it")
@DirtiesContext
class WeatherServiceIntegrationTest {

  // ---- WireMock server (shared across all tests in this class) ----

  static WireMockServer wireMock;

  @BeforeAll
  static void startWireMock() {
    wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    wireMock.start();
  }

  @AfterAll
  static void stopWireMock() {
    wireMock.stop();
  }

  // ---- Dynamic Spring properties pointing Feign clients to WireMock ----

  @DynamicPropertySource
  static void dynamicProperties(DynamicPropertyRegistry registry) {
    String base = "http://localhost:" + wireMock.port();
    registry.add("open-meteo.url", () -> base);
    registry.add("geocoding.url", () -> base);
    registry.add("reverse-geocode.url", () -> base);
  }

  // ---- Test subjects ----

  @Autowired
  private WeatherService weatherService;

  @Autowired
  private CacheManager cacheManager;

  @Autowired
  private FeignHttpMessageConverters feignHttpMessageConverters;

  // ---- Stub payloads ----

  private static final String DAILY_RESPONSE_JSON = """
      {
        "latitude": 52.0,
        "longitude": 4.0,
        "generationtime_ms": 1.0,
        "utc_offset_seconds": 3600,
        "timezone": "Europe/Berlin",
        "timezone_abbreviation": "CET",
        "elevation": 0.0,
        "daily_units": { "time": "iso8601", "weather_code": "wmo code" },
        "daily": {
          "time": ["2026-03-15"],
          "weather_code": [1],
          "temperature_2m_max": [10.0],
          "temperature_2m_min": [2.0],
          "sunrise": ["2026-03-15T06:30"],
          "sunset": ["2026-03-15T18:30"],
          "precipitation_sum": [0.0],
          "precipitation_probability_max": [10],
          "wind_speed_10m_max": [20],
          "wind_direction_10m_dominant": [270]
        }
      }
      """;

  private static final String HOURLY_RESPONSE_JSON = """
      {
        "latitude": 52.0,
        "longitude": 4.0,
        "generationtime_ms": 1.0,
        "utc_offset_seconds": 3600,
        "timezone": "Europe/Berlin",
        "timezone_abbreviation": "CET",
        "elevation": 0.0,
        "current_units": { "time": "iso8601", "temperature_2m": "°C" },
        "current": {
          "time": "2026-03-15T12:00",
          "interval": 900,
          "weather_code": 1,
          "temperature_2m": 8.5,
          "wind_speed_10m": 15,
          "wind_direction_10m": 270
        },
        "hourly_units": { "time": "iso8601", "temperature_2m": "°C" },
        "hourly": {
          "time": ["2026-03-15T00:00"],
          "weather_code": [1],
          "temperature_2m": [8.5],
          "precipitation_probability": [10],
          "precipitation": [0.0],
          "wind_speed_10m": [15],
          "wind_direction_10m": [270]
        }
      }
      """;

  private static final String REVERSE_GEOCODE_RESPONSE_JSON = """
      {
        "city": "Amsterdam",
        "locality": "Amsterdam",
        "countryName": "Netherlands",
        "latitude": 52.0,
        "lookupSource": "coordinates",
        "longitude": 4.0,
        "localityLanguageRequested": "en",
        "continent": "Europe",
        "continentCode": "EU",
        "countryCode": "NL",
        "principalSubdivision": "North Holland",
        "principalSubdivisionCode": "NL-NH",
        "postcode": "1000",
        "plusCode": "9F469XXX+XX",
        "localityInfo": {
          "administrative": [],
          "informative": []
        }
      }
      """;

  // ---- WireMock setup per test ----

  @BeforeEach
  void stubExternalApisAndClearCaches() {
    wireMock.resetAll();

    wireMock.stubFor(get(urlPathMatching("/v1/forecast"))
        .withQueryParam("daily", containing("weather_code"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(DAILY_RESPONSE_JSON)));

    wireMock.stubFor(get(urlPathMatching("/v1/forecast"))
        .withQueryParam("hourly", containing("weather_code"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(HOURLY_RESPONSE_JSON)));

    wireMock.stubFor(get(urlPathMatching("/data/reverse-geocode"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(REVERSE_GEOCODE_RESPONSE_JSON)));

    // Clear all caches before each test to ensure a fresh fetch
    cacheManager.getCacheNames().forEach(name -> {
      Cache cache = cacheManager.getCache(name);
      if (cache != null) {
        cache.clear();
      }
    });
  }

  // ---- Helper: replicate what JdkSerializationRedisSerializer does ----

  private static void assertJdkSerializable(Object object) {
    assertThatCode(() -> {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(object);
      }
    })
        .as("Object of type %s must be JDK-serializable (implements Serializable) "
            + "so that JdkSerializationRedisSerializer can write it to Redis", object.getClass().getSimpleName())
        .doesNotThrowAnyException();
  }

  // ---- Tests ----

  @Test
  @DisplayName("getWeatherDaily() result must be JDK-serializable (regression: NotSerializableException before fix)")
  void getWeatherDaily_resultMustBeJdkSerializable() {
    WeatherResponse response = weatherService.getWeatherDaily(52.0, 4.0);

    assertThat(response).isNotNull();
    // This would have thrown java.io.NotSerializableException before WeatherResponse
    // (and its nested types) implemented Serializable
    assertJdkSerializable(response);
  }

  @Test
  @DisplayName("getWeatherHourly() result must be JDK-serializable (regression: NotSerializableException before fix)")
  void getWeatherHourly_resultMustBeJdkSerializable() {
    WeatherResponse response = weatherService.getWeatherHourly(52.0, 4.0);

    assertThat(response).isNotNull();
    assertJdkSerializable(response);
  }

  @Test
  @DisplayName("getLocation() result must be JDK-serializable (regression: NotSerializableException before fix)")
  void getLocation_resultMustBeJdkSerializable() {
    ReverseGeocodeResponse response = weatherService.getLocation(52.0, 4.0, "en");

    assertThat(response).isNotNull();
    assertThat(response.getCity()).isEqualTo("Amsterdam");
    // This would have thrown java.io.NotSerializableException before
    // ReverseGeocodeResponse (and its nested types) implemented Serializable
    assertJdkSerializable(response);
  }

  @Test
  @DisplayName("Second call for same coordinates should be served from cache (no extra WireMock call)")
  void getWeatherDaily_secondCallServedFromCache() {
    double latitude = 52.0;
    double longitude = 4.0;

    // First call populates cache
    WeatherResponse first = weatherService.getWeatherDaily(latitude, longitude);
    assertThat(first).isNotNull();

    // Reset WireMock stubs — any real HTTP call from now on would get a 404
    wireMock.resetAll();

    // Second call should be served from cache, not from WireMock
    WeatherResponse second = weatherService.getWeatherDaily(latitude, longitude);
    assertThat(second).isNotNull();
    // Both calls return the same cached instance
    assertThat(second).isSameAs(first);
  }

  @Test
  @DisplayName("FeignHttpMessageConverters bean must be eagerly initialised with non-empty converters (regression: DecodeException before fix)")
  void feignHttpMessageConverters_mustBeEagerlyInitialisedWithNonEmptyConverters() {
    // Before the fix, concurrent threads could observe an empty converter list because
    // FeignHttpMessageConverters initialises its list lazily, leading to:
    //   feign.codec.DecodeException: 'messageConverters' must not be empty
    // FeignConfig now calls getConverters() at bean creation time to force eager initialisation.
    assertThat(feignHttpMessageConverters.getConverters())
        .as("FeignHttpMessageConverters must have at least one converter registered")
        .isNotEmpty();
  }
}
