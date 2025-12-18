package nl.markpost.weather.client;

import nl.markpost.weather.config.ReverseGeocodeFeignConfig;
import nl.markpost.weather.model.ReverseGeocodeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "reverseGeocodeClient",
    url = "${reverse-geocode.url}",
    configuration = ReverseGeocodeFeignConfig.class
)
public interface ReverseGeocodeClient {

  @GetMapping("/data/reverse-geocode?localityLanguage=en")
  ReverseGeocodeResponse getLocation(@RequestParam("latitude") double latitude,
      @RequestParam("longitude") double longitude);
}

