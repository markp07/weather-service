package nl.markpost.weather.model;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Administrative implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String name;
  private String description;
  private String isoName;
  private Integer order;
  private Integer adminLevel;
  private String isoCode;
  private String wikidataId;
  private Long geonameId;
}

