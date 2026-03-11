package nl.markpost.weather.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    // Pass empty API key so the service initialises without failing (fetchWarnings will skip gracefully)
    service = new MeteoAlarmService(new RestTemplateBuilder(), "");
  }

  // ---- fetchWarnings early-exit tests ----------------------------------------

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
  @DisplayName("fetchWarnings returns empty list when API key is not configured")
  void fetchWarnings_noApiKey() {
    // Service was created with empty API key, so NL (a supported country) should return empty
    assertTrue(service.fetchWarnings("NL").isEmpty());
  }

  // ---- parseApiResponse tests ------------------------------------------------

  /** Builds a minimal GeoJSON FeatureCollection map for testing. */
  private Map<String, Object> buildFeatureCollection(List<Map<String, Object>> features) {
    Map<String, Object> fc = new HashMap<>();
    fc.put("type", "FeatureCollection");
    fc.put("features", features);
    return fc;
  }

  /** Builds a GeoJSON Feature map. */
  private Map<String, Object> buildFeature(Map<String, Object> properties,
      Map<String, Object> geometry) {
    Map<String, Object> feature = new HashMap<>();
    feature.put("type", "Feature");
    feature.put("properties", properties);
    if (geometry != null) {
      feature.put("geometry", geometry);
    }
    return feature;
  }

  /** Builds a GeoJSON Polygon geometry with the given lon/lat ring. */
  private Map<String, Object> buildPolygonGeometry(List<List<Double>> ring) {
    Map<String, Object> geometry = new HashMap<>();
    geometry.put("type", "Polygon");
    geometry.put("coordinates", List.of(ring));
    return geometry;
  }

  @Test
  @DisplayName("parseApiResponse returns empty list when features is empty")
  void parseApiResponse_emptyFeatures() {
    Map<String, Object> response = buildFeatureCollection(List.of());
    assertTrue(service.parseApiResponse(response).isEmpty());
  }

  @Test
  @DisplayName("parseApiResponse returns empty list when features key is missing")
  void parseApiResponse_missingFeaturesKey() {
    Map<String, Object> response = new HashMap<>();
    assertTrue(service.parseApiResponse(response).isEmpty());
  }

  @Test
  @DisplayName("parseApiResponse extracts all CAP fields from a valid feature")
  void parseApiResponse_allCapFields() {
    Map<String, Object> props = new HashMap<>();
    props.put("awareness_level", "3; orange; Severe");
    props.put("awareness_type", "4; Wind");
    props.put("event", "Wind");
    props.put("severity", "Severe");
    props.put("certainty", "Likely");
    props.put("urgency", "Expected");
    props.put("senderName", "KNMI");
    props.put("headline", "Orange warning for Wind");
    props.put("description", "Strong winds expected.");
    props.put("instruction", "Avoid travel if possible.");
    props.put("areaDesc", "Noord-Holland");
    props.put("onset", "2025-11-04T12:00:00+01:00");
    props.put("expires", "2025-11-05T00:00:00+01:00");
    props.put("effective", "2025-11-04T10:00:00+01:00");

    // GeoJSON polygon: [lon, lat] order → roughly 4..5 lon, 52..53 lat
    List<List<Double>> ring = List.of(
        List.of(4.0, 52.0), List.of(5.0, 52.0), List.of(5.0, 53.0),
        List.of(4.0, 53.0), List.of(4.0, 52.0));
    Map<String, Object> feature = buildFeature(props, buildPolygonGeometry(ring));

    List<MeteoAlarmWarning> warnings =
        service.parseApiResponse(buildFeatureCollection(List.of(feature)));

    assertEquals(1, warnings.size());
    MeteoAlarmWarning w = warnings.get(0);
    assertEquals("3; orange; Severe", w.getAwarenessLevel());
    assertEquals("Wind", w.getAwarenessType()); // parsed from "4; Wind"
    assertEquals("Wind", w.getEvent());
    assertEquals("Severe", w.getSeverity());
    assertEquals("Likely", w.getCertainty());
    assertEquals("Expected", w.getUrgency());
    assertEquals("KNMI", w.getSenderName());
    assertEquals("Orange warning for Wind", w.getHeadline());
    assertEquals("Strong winds expected.", w.getDescription());
    assertEquals("Avoid travel if possible.", w.getInstruction());
    assertEquals("Noord-Holland", w.getAreaDesc());
    assertNotNull(w.getOnset());
    assertNotNull(w.getExpires());
    assertNotNull(w.getEffective());
    // Polygon should be converted to CAP text "lat,lon …" format
    assertNotNull(w.getPolygon());
    assertTrue(w.getPolygon().contains("52.0,4.0"));
  }

  @Test
  @DisplayName("parseApiResponse skips feature with no awarenessLevel")
  void parseApiResponse_skipsWithoutAwarenessLevel() {
    Map<String, Object> props = new HashMap<>();
    props.put("event", "Wind");
    props.put("areaDesc", "Noord-Holland");
    Map<String, Object> feature = buildFeature(props, null);

    List<MeteoAlarmWarning> warnings =
        service.parseApiResponse(buildFeatureCollection(List.of(feature)));

    assertTrue(warnings.isEmpty());
  }

  @Test
  @DisplayName("parseApiResponse skips feature with no properties")
  void parseApiResponse_skipsNoProperties() {
    Map<String, Object> feature = new HashMap<>();
    feature.put("type", "Feature");
    // no "properties" key

    List<MeteoAlarmWarning> warnings =
        service.parseApiResponse(buildFeatureCollection(List.of(feature)));

    assertTrue(warnings.isEmpty());
  }

  @Test
  @DisplayName("parseApiResponse handles feature without geometry gracefully")
  void parseApiResponse_noGeometry() {
    Map<String, Object> props = new HashMap<>();
    props.put("awareness_level", "2; yellow; Moderate");
    props.put("areaDesc", "Germany");
    Map<String, Object> feature = buildFeature(props, null);

    List<MeteoAlarmWarning> warnings =
        service.parseApiResponse(buildFeatureCollection(List.of(feature)));

    assertEquals(1, warnings.size());
    assertNull(warnings.get(0).getPolygon()); // no geometry → no polygon
  }

  @Test
  @DisplayName("parseApiResponse converts GeoJSON polygon [lon,lat] to CAP text lat,lon")
  void parseApiResponse_polygonConversion() {
    Map<String, Object> props = new HashMap<>();
    props.put("awareness_level", "3; orange; Severe");
    // GeoJSON polygon ring: [lon=4.0,lat=52.0], [lon=5.0,lat=52.0], [lon=5.0,lat=53.0], ...
    List<List<Double>> ring = List.of(
        List.of(4.5, 52.5), List.of(5.5, 52.5), List.of(5.5, 53.5),
        List.of(4.5, 53.5), List.of(4.5, 52.5));
    Map<String, Object> feature = buildFeature(props, buildPolygonGeometry(ring));

    List<MeteoAlarmWarning> warnings =
        service.parseApiResponse(buildFeatureCollection(List.of(feature)));

    assertNotNull(warnings.get(0).getPolygon());
    // CAP format should be lat,lon: 52.5,4.5 (lat=52.5, lon=4.5)
    assertTrue(warnings.get(0).getPolygon().contains("52.5,4.5"));
  }

  // ---- resolveHighestActive tests --------------------------------------------

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

  // ---- filterByRegion tests --------------------------------------------------

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
    List<MeteoAlarmWarning> result = service.filterByRegion(warnings, "Noord-Holland");
    assertEquals(warnings, result);
  }

  @Test
  @DisplayName("filterByRegion returns all warnings when areaDesc is null")
  void filterByRegion_nullAreaDesc_fallsBack() {
    List<MeteoAlarmWarning> warnings = List.of(
        MeteoAlarmWarning.builder().awarenessLevel("3; orange; Severe").build()
    );
    List<MeteoAlarmWarning> result = service.filterByRegion(warnings, "Noord-Holland");
    assertEquals(warnings, result);
  }

  // ---- resolveAlarmForDay tests ----------------------------------------------

  @Test
  @DisplayName("resolveAlarmForDay returns null when no alarm overlaps day")
  void resolveAlarmForDay_noOverlap() {
    OffsetDateTime now = OffsetDateTime.now();
    LocalDate tomorrow = LocalDate.now().plusDays(1);
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
    // Square polygon covering lat 52..53, lon 4..5 (CAP text format)
    String polygon = "52.0,4.0 53.0,4.0 53.0,5.0 52.0,5.0 52.0,4.0";
    List<MeteoAlarmWarning> warnings = List.of(
        MeteoAlarmWarning.builder().awarenessLevel("3; orange; Severe")
            .areaDesc("Noord-Holland").polygon(polygon).build()
    );
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
    List<MeteoAlarmWarning> result =
        service.filterForLocation(warnings, 52.5, 4.5, "Unknown-Region");
    assertEquals(warnings, result);
  }

}
