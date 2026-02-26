package nl.markpost.weather.model;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents a single weather warning from the MeteoAlarm CAP/Atom feed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeteoAlarmWarning {

  /**
   * Awareness level string from CAP data (e.g. "2; yellow; Moderate") or entry title keyword.
   */
  private String awarenessLevel;

  /**
   * Awareness type from CAP data (e.g. "Wind", "Rain/Flood", "Thunderstorm").
   */
  private String awarenessType;

  /**
   * Short headline from the CAP alert (e.g. "Orange warning for Wind").
   */
  private String headline;

  /**
   * Full description of the warning from the CAP alert.
   */
  private String description;

  /**
   * Area description from the CAP alert (e.g. "Netherlands: Noord-Holland").
   */
  private String areaDesc;

  /**
   * Warning onset time (null when not available in the Atom entry).
   */
  private OffsetDateTime onset;

  /**
   * Warning expiry time (null when not available in the Atom entry).
   */
  private OffsetDateTime expires;

}
