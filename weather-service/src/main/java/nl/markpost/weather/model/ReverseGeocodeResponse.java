package nl.markpost.weather.model;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReverseGeocodeResponse implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String city;

  private String locality;

  private String countryName;

  private double latitude;

  private String lookupSource;

  private Double longitude;

  private String localityLanguageRequested;

  private String continent;

  private String continentCode;

  private String countryCode;

  private String principalSubdivision;

  private String principalSubdivisionCode;

  private String postcode;

  private String plusCode;

  private LocalityInfo localityInfo;
}
