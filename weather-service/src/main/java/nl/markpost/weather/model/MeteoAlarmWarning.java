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
   * CAP event name (e.g. "Wind", "Thunderstorm") from {@code <cap:event>}.
   */
  private String event;

  /**
   * CAP severity (e.g. "Severe", "Extreme") from {@code <cap:severity>}.
   */
  private String severity;

  /**
   * CAP certainty (e.g. "Likely", "Observed") from {@code <cap:certainty>}.
   */
  private String certainty;

  /**
   * CAP urgency (e.g. "Immediate", "Expected") from {@code <cap:urgency>}.
   */
  private String urgency;

  /**
   * Name of the issuing national meteorological service from {@code <cap:senderName>}.
   */
  private String senderName;

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
   * CAP polygon as a space-separated list of "lat,lon" coordinate pairs defining the
   * geographic area covered by this warning. Null when not present in the feed.
   */
  private String polygon;

  /**
   * Warning onset time (null when not available in the Atom entry).
   */
  private OffsetDateTime onset;

  /**
   * Warning expiry time (null when not available in the Atom entry).
   */
  private OffsetDateTime expires;

}
