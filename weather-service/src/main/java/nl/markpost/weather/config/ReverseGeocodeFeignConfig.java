package nl.markpost.weather.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Feign configuration for the ReverseGeocodeClient.
 * Adds the API key to all requests automatically.
 */
public class ReverseGeocodeFeignConfig {

  @Value("${reverse-geocode.api-key}")
  private String apiKey;

  /**
   * Request interceptor that adds the API key as a query parameter to all requests.
   */
  @Bean
  public RequestInterceptor requestInterceptor() {
    return requestTemplate -> requestTemplate.query("key", apiKey);
  }
}

