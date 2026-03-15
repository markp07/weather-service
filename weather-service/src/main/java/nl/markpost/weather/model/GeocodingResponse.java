package nl.markpost.weather.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from the Open-Meteo Geocoding API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeocodingResponse implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private List<GeocodingResult> results;
}
