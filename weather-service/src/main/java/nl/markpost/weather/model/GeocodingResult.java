package nl.markpost.weather.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model for a single geocoding search result from the Open-Meteo Geocoding API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeocodingResult implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private Long id;

  private String name;

  private Double latitude;

  private Double longitude;

  private Double elevation;

  @JsonProperty("feature_code")
  private String featureCode;

  @JsonProperty("country_code")
  private String countryCode;

  private String country;

  private String timezone;

  private Long population;

  private String admin1;

  @JsonProperty("admin1_id")
  private Long admin1Id;

  private String admin2;

  @JsonProperty("admin2_id")
  private Long admin2Id;

  private String admin3;

  @JsonProperty("admin3_id")
  private Long admin3Id;

  private String admin4;

  @JsonProperty("admin4_id")
  private Long admin4Id;
}
