package nl.markpost.weather.model;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single weather warning from the MeteoAlarm CAP/Atom feed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeteoAlarmWarning {

  /**
   * Awareness level string from CAP data (e.g. "2; yellow; Moderate") or entry title keyword.
   */
  private String awarenessLevel;

  /**
   * Warning onset time (null when not available in the Atom entry).
   */
  private OffsetDateTime onset;

  /**
   * Warning expiry time (null when not available in the Atom entry).
   */
  private OffsetDateTime expires;

}
