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
  @DisplayName("parseAtomFeed extracts all CAP fields including new ones")
  void parseAtomFeed_allCapFields() {
    String xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <entry>
            <title>Weather warning</title>
            <alert xmlns="urn:oasis:names:tc:emergency:cap:1.2">
              <senderName xmlns="urn:oasis:names:tc:emergency:cap:1.2">KNMI</senderName>
              <info xmlns="urn:oasis:names:tc:emergency:cap:1.2">
                <event xmlns="urn:oasis:names:tc:emergency:cap:1.2">Wind</event>
                <severity xmlns="urn:oasis:names:tc:emergency:cap:1.2">Severe</severity>
                <certainty xmlns="urn:oasis:names:tc:emergency:cap:1.2">Likely</certainty>
                <urgency xmlns="urn:oasis:names:tc:emergency:cap:1.2">Expected</urgency>
                <onset xmlns="urn:oasis:names:tc:emergency:cap:1.2">2025-11-04T12:00:00+01:00</onset>
                <expires xmlns="urn:oasis:names:tc:emergency:cap:1.2">2025-11-05T00:00:00+01:00</expires>
                <headline xmlns="urn:oasis:names:tc:emergency:cap:1.2">Orange warning for Wind</headline>
                <description xmlns="urn:oasis:names:tc:emergency:cap:1.2">Strong winds expected.</description>
                <area xmlns="urn:oasis:names:tc:emergency:cap:1.2">
                  <areaDesc xmlns="urn:oasis:names:tc:emergency:cap:1.2">Noord-Holland</areaDesc>
                  <polygon xmlns="urn:oasis:names:tc:emergency:cap:1.2">52.5,4.5 53.0,4.5 53.0,5.0 52.5,5.0 52.5,4.5</polygon>
                </area>
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
    MeteoAlarmWarning w = warnings.get(0);
    assertEquals("3; orange; Severe", w.getAwarenessLevel());
    assertEquals("Wind", w.getEvent());
    assertEquals("Severe", w.getSeverity());
    assertEquals("Likely", w.getCertainty());
    assertEquals("Expected", w.getUrgency());
    assertEquals("KNMI", w.getSenderName());
    assertEquals("Orange warning for Wind", w.getHeadline());
    assertEquals("Strong winds expected.", w.getDescription());
    assertEquals("Noord-Holland", w.getAreaDesc());
    assertNotNull(w.getPolygon());
    assertTrue(w.getPolygon().contains("52.5,4.5"));
    assertNotNull(w.getOnset());
    assertNotNull(w.getExpires());
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
        MeteoAlarmWarning.builder().awarenessLevel("2; yellow; Moderate")
            .onset(now.minusHours(1)).expires(now.plusHours(1)).build(),
        MeteoAlarmWarning.builder().awarenessLevel("3; orange; Severe")
            .onset(now.minusHours(1)).expires(now.plusHours(1)).build()
    );
    assertEquals(WeatherAlarm.ORANGE, service.resolveHighestActive(warnings, now));
  }

  @Test
  @DisplayName("resolveHighestActive ignores expired warnings")
  void resolveHighestActive_expiredIgnored() {
    OffsetDateTime now = OffsetDateTime.now();
    List<MeteoAlarmWarning> warnings = List.of(
        MeteoAlarmWarning.builder().awarenessLevel("4; red; Extreme")
            .onset(now.minusHours(2)).expires(now.minusHours(1)).build()
    );
    assertEquals(WeatherAlarm.GREEN, service.resolveHighestActive(warnings, now));
  }

  @Test
  @DisplayName("resolveHighestActive treats warning with no onset/expires as active")
  void resolveHighestActive_noTimeRange() {
    OffsetDateTime now = OffsetDateTime.now();
    List<MeteoAlarmWarning> warnings = List.of(
        MeteoAlarmWarning.builder().awarenessLevel("Yellow warning for Wind").build()
    );
    assertEquals(WeatherAlarm.YELLOW, service.resolveHighestActive(warnings, now));
  }

  @Test
  @DisplayName("filterByRegion returns all warnings when subdivision is null")
  void filterByRegion_nullSubdivision() {
    List<MeteoAlarmWarning> warnings = List.of(
        MeteoAlarmWarning.builder().awarenessLevel("3; orange; Severe")
            .areaDesc("Netherlands: Noord-Holland").build()
    );
    assertEquals(warnings, service.filterByRegion(warnings, null));
  }

  @Test
  @DisplayName("filterByRegion returns all warnings when subdivision is blank")
  void filterByRegion_blankSubdivision() {
    List<MeteoAlarmWarning> warnings = List.of(
        MeteoAlarmWarning.builder().awarenessLevel("3; orange; Severe")
            .areaDesc("Netherlands: Noord-Holland").build()
    );
    assertEquals(warnings, service.filterByRegion(warnings, "  "));
  }

  @Test
  @DisplayName("filterByRegion returns only matching regional warnings")
  void filterByRegion_matchingRegion() {
    List<MeteoAlarmWarning> warnings = List.of(
        MeteoAlarmWarning.builder().awarenessLevel("2; yellow; Moderate")
            .areaDesc("Netherlands: Noord-Holland").build(),
        MeteoAlarmWarning.builder().awarenessLevel("3; orange; Severe")
            .areaDesc("Netherlands: Zuid-Holland").build()
    );
    List<MeteoAlarmWarning> result = service.filterByRegion(warnings, "Noord-Holland");
    assertEquals(1, result.size());
    assertEquals("Netherlands: Noord-Holland", result.get(0).getAreaDesc());
  }

  @Test
  @DisplayName("filterByRegion is case-insensitive")
  void filterByRegion_caseInsensitive() {
    List<MeteoAlarmWarning> warnings = List.of(
        MeteoAlarmWarning.builder().awarenessLevel("3; orange; Severe")
            .areaDesc("Netherlands: Noord-Holland").build()
    );
    assertEquals(1, service.filterByRegion(warnings, "noord-holland").size());
  }

  @Test
  @DisplayName("filterByRegion falls back to all warnings when no match for subdivision")
  void filterByRegion_noMatch_fallsBack() {
    List<MeteoAlarmWarning> warnings = List.of(
        MeteoAlarmWarning.builder().awarenessLevel("3; orange; Severe")
            .areaDesc("Netherlands: Gelderland").build()
    );
    // Noord-Holland not in areaDesc: fall back to country-level
    List<MeteoAlarmWarning> result = service.filterByRegion(warnings, "Noord-Holland");
    assertEquals(warnings, result);
  }

  @Test
  @DisplayName("filterByRegion returns all warnings when areaDesc is null")
  void filterByRegion_nullAreaDesc_fallsBack() {
    List<MeteoAlarmWarning> warnings = List.of(
        MeteoAlarmWarning.builder().awarenessLevel("3; orange; Severe").build()
    );
    // No areaDesc, subdivision supplied: fall back to country-level
    List<MeteoAlarmWarning> result = service.filterByRegion(warnings, "Noord-Holland");
    assertEquals(warnings, result);
  }

  @Test
  @DisplayName("resolveAlarmForDay returns null when no alarm overlaps day")
  void resolveAlarmForDay_noOverlap() {
    OffsetDateTime now = OffsetDateTime.now();
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    // Warning only covers today
    List<MeteoAlarmWarning> warnings = List.of(
        MeteoAlarmWarning.builder().awarenessLevel("2; yellow; Moderate")
            .onset(now.minusHours(1)).expires(now.plusHours(1)).build()
    );
    assertNull(service.resolveAlarmForDay(warnings, tomorrow, now));
  }

  @Test
  @DisplayName("resolveAlarmForDay returns alarm when warning overlaps the day")
  void resolveAlarmForDay_overlaps() {
    OffsetDateTime now = OffsetDateTime.now();
    LocalDate today = LocalDate.now();
    List<MeteoAlarmWarning> warnings = List.of(
        MeteoAlarmWarning.builder().awarenessLevel("3; orange; Severe")
            .onset(now.minusHours(1)).expires(now.plusHours(3)).build()
    );
    assertEquals(WeatherAlarm.ORANGE, service.resolveAlarmForDay(warnings, today, now));
  }

  // ---- filterForLocation tests ------------------------------------------------

  @Test
  @DisplayName("filterForLocation returns polygon match when point is inside polygon")
  void filterForLocation_polygonMatch() {
    // Square polygon covering lat 52..53, lon 4..5
    String polygon = "52.0,4.0 53.0,4.0 53.0,5.0 52.0,5.0 52.0,4.0";
    List<MeteoAlarmWarning> warnings = List.of(
        MeteoAlarmWarning.builder().awarenessLevel("3; orange; Severe")
            .areaDesc("Noord-Holland").polygon(polygon).build()
    );
    // Point at 52.5, 4.5 is inside the polygon
    List<MeteoAlarmWarning> result = service.filterForLocation(warnings, 52.5, 4.5, null);
    assertEquals(1, result.size());
  }

  @Test
  @DisplayName("filterForLocation excludes warning when point is outside polygon")
  void filterForLocation_polygonNoMatch_fallsBackToSubdivision() {
    String polygon = "52.0,4.0 53.0,4.0 53.0,5.0 52.0,5.0 52.0,4.0";
    List<MeteoAlarmWarning> warnings = List.of(
        MeteoAlarmWarning.builder().awarenessLevel("3; orange; Severe")
            .areaDesc("Noord-Holland").polygon(polygon).build(),
        MeteoAlarmWarning.builder().awarenessLevel("2; yellow; Moderate")
            .areaDesc("Zuid-Holland").build()
    );
    // Point at 51.9, 4.4 is outside the polygon; only the non-polygon warning with matching
    // subdivision (or country fallback) should be returned
    List<MeteoAlarmWarning> result = service.filterForLocation(warnings, 51.9, 4.4, "Zuid-Holland");
    assertEquals(1, result.size());
    assertEquals("Zuid-Holland", result.get(0).getAreaDesc());
  }

  @Test
  @DisplayName("filterForLocation falls back to all warnings when no polygon or subdivision match")
  void filterForLocation_noMatch_countryFallback() {
    List<MeteoAlarmWarning> warnings = List.of(
        MeteoAlarmWarning.builder().awarenessLevel("3; orange; Severe")
            .areaDesc("Netherlands").build()
    );
    // No polygon, subdivision not matching areaDesc: should get all warnings as fallback
    List<MeteoAlarmWarning> result = service.filterForLocation(warnings, 52.5, 4.5, "Unknown-Region");
    assertEquals(warnings, result);
  }

}
