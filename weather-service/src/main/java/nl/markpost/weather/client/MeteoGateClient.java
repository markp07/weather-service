package nl.markpost.weather.client;

import java.util.Map;
import nl.markpost.weather.config.MeteoGateFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for the MeteoGate Warnings OGC API EDR.
 * Fetches official weather warnings from the EUMETNET MeteoGate open data platform.
 * Authentication is handled via {@link MeteoGateFeignConfig} (Bearer token interceptor).
 */
@FeignClient(
    name = "meteoGateClient",
    url = "${meteoalarm.url}",
    configuration = MeteoGateFeignConfig.class
)
public interface MeteoGateClient {

  /**
   * Retrieves warning features for the given country code as a GeoJSON FeatureCollection.
   *
   * @param locationId ISO 3166-1 alpha-2 country code (e.g. "NL")
   * @param datetime   required ISO 8601 interval (e.g. "2025-01-01T00:00:00Z/2025-01-15T00:00:00Z")
   * @return GeoJSON FeatureCollection as a raw map
   */
  @GetMapping("/collections/warnings/locations/{locationId}")
  Map<String, Object> getWarnings(
      @PathVariable("locationId") String locationId,
      @RequestParam("datetime") String datetime
  );
}
