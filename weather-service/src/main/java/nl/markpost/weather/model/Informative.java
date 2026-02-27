package nl.markpost.weather.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Informative {

  private String name;
  private String description;
  private Integer order;
  private String isoCode;
  private String wikidataId;
  private Long geonameId;
}

