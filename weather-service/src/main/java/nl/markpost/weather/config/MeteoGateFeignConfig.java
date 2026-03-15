package nl.markpost.weather.config;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Feign configuration for the MeteoGateClient.
 * Adds the MeteoGate API key as a Bearer token Authorization header to all requests.
 * Enables FULL Feign logging (request line, headers, body; response status, headers, body).
 */
public class MeteoGateFeignConfig {

  @Value("${meteoalarm.api-key:}")
  private String apiKey;

  /**
   * Request interceptor that adds the Bearer token to all outgoing requests.
   */
  @Bean
  public RequestInterceptor meteoGateAuthInterceptor() {
    return requestTemplate -> {
      if (apiKey != null && !apiKey.isBlank()) {
        requestTemplate.header("Authorization", "Bearer " + apiKey);
      }
    };
  }

  /**
   * Enables full Feign logging: request line + headers + body, response status + headers + body.
   * Requires the logger for {@code nl.markpost.weather.client.MeteoGateClient} to be set to DEBUG.
   */
  @Bean
  public Logger.Level meteoGateFeignLoggerLevel() {
    return Logger.Level.FULL;
  }
}
