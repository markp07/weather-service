package nl.markpost.weather.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Administrative {

  private String name;
  private String description;
  private String isoName;
  private Integer order;
  private Integer adminLevel;
  private String isoCode;
  private String wikidataId;
  private Long geonameId;
}

