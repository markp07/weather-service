package nl.markpost.weather.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Feign configuration for the MeteoGateClient.
 * Adds the MeteoGate API key as a Bearer token Authorization header to all requests.
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
}
