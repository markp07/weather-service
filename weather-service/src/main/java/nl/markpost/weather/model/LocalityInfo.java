package nl.markpost.weather.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocalityInfo implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private List<Administrative> administrative;
  private List<Informative> informative;
}

