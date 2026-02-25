package nl.markpost.weather.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import nl.markpost.weather.model.MeteoAlarmWarning;
import nl.markpost.weather.model.WeatherAlarm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

class MeteoAlarmServiceTest {

  private MeteoAlarmService service;

  @BeforeEach
  void setUp() {
    service = new MeteoAlarmService(new RestTemplateBuilder());
  }

  @Test
  @DisplayName("fetchWarnings returns empty list for null countryCode")
  void fetchWarnings_null() {
    assertTrue(service.fetchWarnings(null).isEmpty());
  }

  @Test
  @DisplayName("fetchWarnings returns empty list for unsupported country")
  void fetchWarnings_unsupportedCountry() {
    assertTrue(service.fetchWarnings("US").isEmpty());
    assertTrue(service.fetchWarnings("XX").isEmpty());
  }

  @Test
  @DisplayName("parseAtomFeed extracts awareness_level from embedded CAP parameters")
  void parseAtomFeed_capParameters() {
    String xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <entry>
            <title>Weather warning</title>
            <alert xmlns="urn:oasis:names:tc:emergency:cap:1.2">
              <info xmlns="urn:oasis:names:tc:emergency:cap:1.2">
                <onset xmlns="urn:oasis:names:tc:emergency:cap:1.2">2025-11-04T12:00:00+01:00</onset>
                <expires xmlns="urn:oasis:names:tc:emergency:cap:1.2">2025-11-05T00:00:00+01:00</expires>
                <parameter xmlns="urn:oasis:names:tc:emergency:cap:1.2">
                  <valueName xmlns="urn:oasis:names:tc:emergency:cap:1.2">awareness_level</valueName>
                  <value xmlns="urn:oasis:names:tc:emergency:cap:1.2">3; orange; Severe</value>
                </parameter>
              </info>
            </alert>
          </entry>
        </feed>
        """;
    List<MeteoAlarmWarning> warnings = service.parseAtomFeed(xml);
    assertNotNull(warnings);
    assertEquals(1, warnings.size());
    assertEquals("3; orange; Severe", warnings.get(0).getAwarenessLevel());
    assertNotNull(warnings.get(0).getOnset());
    assertNotNull(warnings.get(0).getExpires());
  }

  @Test
  @DisplayName("parseAtomFeed falls back to entry title when no CAP parameters")
  void parseAtomFeed_titleFallback() {
    String xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <entry>
            <title>Yellow warning for Wind</title>
            <link type="application/cap+xml" href="https://example.com/cap.xml"/>
          </entry>
        </feed>
        """;
    List<MeteoAlarmWarning> warnings = service.parseAtomFeed(xml);
    assertNotNull(warnings);
    assertEquals(1, warnings.size());
    assertEquals("Yellow warning for Wind", warnings.get(0).getAwarenessLevel());
    assertNull(warnings.get(0).getOnset());
    assertNull(warnings.get(0).getExpires());
  }

  @Test
  @DisplayName("parseAtomFeed returns empty list for malformed XML")
  void parseAtomFeed_malformed() {
    assertTrue(service.parseAtomFeed("<not valid xml<<<").isEmpty());
  }

  @Test
  @DisplayName("parseAtomFeed returns empty list for empty feed")
  void parseAtomFeed_empty() {
    String xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
        </feed>
        """;
    assertTrue(service.parseAtomFeed(xml).isEmpty());
  }

  @Test
  @DisplayName("resolveHighestActive returns GREEN when no warnings")
  void resolveHighestActive_noWarnings() {
    assertEquals(WeatherAlarm.GREEN,
        service.resolveHighestActive(List.of(), OffsetDateTime.now()));
  }

  @Test
  @DisplayName("resolveHighestActive returns highest alarm for active warnings")
  void resolveHighestActive_multipleWarnings() {
    OffsetDateTime now = OffsetDateTime.now();
    List<MeteoAlarmWarning> warnings = List.of(
        new MeteoAlarmWarning("2; yellow; Moderate", now.minusHours(1), now.plusHours(1)),
        new MeteoAlarmWarning("3; orange; Severe", now.minusHours(1), now.plusHours(1))
    );
    assertEquals(WeatherAlarm.ORANGE, service.resolveHighestActive(warnings, now));
  }

  @Test
  @DisplayName("resolveHighestActive ignores expired warnings")
  void resolveHighestActive_expiredIgnored() {
    OffsetDateTime now = OffsetDateTime.now();
    List<MeteoAlarmWarning> warnings = List.of(
        new MeteoAlarmWarning("4; red; Extreme", now.minusHours(2), now.minusHours(1))
    );
    assertEquals(WeatherAlarm.GREEN, service.resolveHighestActive(warnings, now));
  }

  @Test
  @DisplayName("resolveHighestActive treats warning with no onset/expires as active")
  void resolveHighestActive_noTimeRange() {
    OffsetDateTime now = OffsetDateTime.now();
    List<MeteoAlarmWarning> warnings = List.of(
        new MeteoAlarmWarning("Yellow warning for Wind", null, null)
    );
    assertEquals(WeatherAlarm.YELLOW, service.resolveHighestActive(warnings, now));
  }

  @Test
  @DisplayName("resolveAlarmForDay returns null when no alarm overlaps day")
  void resolveAlarmForDay_noOverlap() {
    OffsetDateTime now = OffsetDateTime.now();
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    // Warning only covers today
    List<MeteoAlarmWarning> warnings = List.of(
        new MeteoAlarmWarning("2; yellow; Moderate", now.minusHours(1), now.plusHours(1))
    );
    assertNull(service.resolveAlarmForDay(warnings, tomorrow, now));
  }

  @Test
  @DisplayName("resolveAlarmForDay returns alarm when warning overlaps the day")
  void resolveAlarmForDay_overlaps() {
    OffsetDateTime now = OffsetDateTime.now();
    LocalDate today = LocalDate.now();
    List<MeteoAlarmWarning> warnings = List.of(
        new MeteoAlarmWarning("3; orange; Severe", now.minusHours(1), now.plusHours(3))
    );
    assertEquals(WeatherAlarm.ORANGE, service.resolveAlarmForDay(warnings, today, now));
  }

}
