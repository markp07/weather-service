package nl.markpost.weather.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import nl.markpost.weather.client.MeteoGateClient;
import nl.markpost.weather.model.MeteoAlarmWarning;
import nl.markpost.weather.model.WeatherAlarm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service for fetching official weather alarms from the MeteoGate Warnings API.
 * MeteoGate is the open EUMETNET data access platform, providing access to warnings from 35+
 * European National Meteorological and Hydrological Services via the OGC API EDR standard.
 * API: https://api.meteogate.eu/warnings/collections/warnings/locations/{countryCode}
 * Authentication: Authorization: Bearer {token}
 */
@Slf4j
@Service
public class MeteoAlarmService {

  /**
   * ISO 3166-1 alpha-2 country codes supported by the MeteoGate Warnings API.
   */
  private static final Set<String> SUPPORTED_COUNTRIES = Set.of(
      "AT", "BE", "BA", "BG", "HR", "CY", "CZ", "DK", "EE", "FI",
      "FR", "DE", "GR", "HU", "IS", "IE", "IL", "IT", "LV", "LT",
      "LU", "MT", "MD", "ME", "NL", "MK", "NO", "PL", "PT", "RO",
      "RS", "SK", "SI", "ES", "SE", "CH", "UA", "UK"
  );

  private final MeteoGateClient meteoGateClient;
  private final boolean apiKeyConfigured;

  public MeteoAlarmService(MeteoGateClient meteoGateClient,
      @Value("${meteoalarm.api-key:}") String apiKey) {
    this.meteoGateClient = meteoGateClient;
    this.apiKeyConfigured = apiKey != null && !apiKey.isBlank();
    if (!this.apiKeyConfigured) {
      log.warn("MeteoGate API key is not configured (METEOALARM_API_KEY) — "
          + "weather alarm fetching is disabled");
    }
  }

  /**
   * Returns the highest active weather alarm level for the given location, or GREEN if no alarm
   * is active or the country is not covered by MeteoGate.
   * Uses point-in-polygon filtering (preferred) or subdivision text-matching as fallback.
   *
   * @param countryCode ISO 3166-1 alpha-2 country code (e.g. "NL")
   * @param latitude    user latitude for polygon-based filtering
   * @param longitude   user longitude for polygon-based filtering
   * @param subdivision principal subdivision / province name (may be null)
   * @return the highest active WeatherAlarm level
   */
  @Cacheable(value = "weatherAlarms", key = "#countryCode + ':' + T(String).format('%.2f', T(Math).round(#latitude / 0.05) * 0.05D) + ',' + T(String).format('%.2f', T(Math).round(#longitude / 0.05) * 0.05D)")
  public WeatherAlarm getHighestAlarm(String countryCode, double latitude, double longitude,
      String subdivision) {
    List<MeteoAlarmWarning> warnings = filterForLocation(fetchWarnings(countryCode), latitude,
        longitude, subdivision);
    return resolveHighestActive(warnings, OffsetDateTime.now(ZoneOffset.UTC));
  }

  /**
   * Returns a list of active warnings for each daily entry date, or null when no alarm is active
   * on a given day. Uses point-in-polygon filtering (preferred) or subdivision text-matching.
   *
   * @param countryCode ISO 3166-1 alpha-2 country code
   * @param latitude    user latitude for polygon-based filtering
   * @param longitude   user longitude for polygon-based filtering
   * @param subdivision principal subdivision / province name (may be null)
   * @param dates       list of daily dates to check alarms for
   * @return list of WeatherAlarm levels (same size as dates), null entries where no alarm is active
   */
  @Cacheable(value = "weatherAlarms", key = "#countryCode + ':' + T(String).format('%.2f', T(Math).round(#latitude / 0.05) * 0.05D) + ',' + T(String).format('%.2f', T(Math).round(#longitude / 0.05) * 0.05D) + '-daily'")
  public List<WeatherAlarm> getDailyAlarms(String countryCode, double latitude, double longitude,
      String subdivision, List<LocalDate> dates) {
    List<MeteoAlarmWarning> warnings = filterForLocation(fetchWarnings(countryCode), latitude,
        longitude, subdivision);
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    List<WeatherAlarm> result = new ArrayList<>();
    for (LocalDate date : dates) {
      result.add(resolveAlarmForDay(warnings, date, now));
    }
    return result;
  }

  /**
   * Returns all currently active warning objects for the given location.
   * Uses point-in-polygon filtering (preferred) or subdivision text-matching.
   * Returns an empty list when the country is not covered by MeteoGate or no alarms are active.
   *
   * @param countryCode ISO 3166-1 alpha-2 country code
   * @param latitude    user latitude for polygon-based filtering
   * @param longitude   user longitude for polygon-based filtering
   * @param subdivision principal subdivision / province name (may be null)
   * @return list of currently active MeteoAlarmWarning objects
   */
  @Cacheable(value = "weatherAlarms", key = "#countryCode + ':' + T(String).format('%.2f', T(Math).round(#latitude / 0.05) * 0.05D) + ',' + T(String).format('%.2f', T(Math).round(#longitude / 0.05) * 0.05D) + '-active'")
  public List<MeteoAlarmWarning> getActiveWarnings(String countryCode, double latitude,
      double longitude, String subdivision) {
    List<MeteoAlarmWarning> warnings = filterForLocation(fetchWarnings(countryCode), latitude,
        longitude, subdivision);
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    List<MeteoAlarmWarning> active = new ArrayList<>();
    for (MeteoAlarmWarning warning : warnings) {
      if (isActive(warning, now)) {
        active.add(warning);
      }
    }
    return active;
  }

  /**
   * Filters warnings to those relevant for the given subdivision (province/region).
   * Matching is done by checking whether the warning's {@code areaDesc} contains the subdivision
   * name (case-insensitive). If {@code subdivision} is blank, or if no warning mentions the
   * subdivision, all warnings are returned unchanged (country-level fallback).
   */
  List<MeteoAlarmWarning> filterByRegion(List<MeteoAlarmWarning> warnings, String subdivision) {
    if (subdivision == null || subdivision.isBlank() || warnings.isEmpty()) {
      return warnings;
    }
    String sub = subdivision.trim().toLowerCase();
    List<MeteoAlarmWarning> regional = warnings.stream()
        .filter(w -> w.getAreaDesc() != null && w.getAreaDesc().toLowerCase().contains(sub))
        .collect(java.util.stream.Collectors.toList());
    if (regional.isEmpty()) {
      log.debug("No warnings matched subdivision '{}' — falling back to all {} country warning(s)",
          subdivision, warnings.size());
    } else {
      log.debug("Subdivision filter '{}' narrowed {} warning(s) down to {}", subdivision,
          warnings.size(), regional.size());
    }
    return regional.isEmpty() ? warnings : regional;
  }

  /**
   * Filters warnings to the most specific geographic match possible.
   * <p>
   * Priority:
   * <ol>
   *   <li>Point-in-polygon: if any warnings carry a CAP {@code polygon}, check whether
   *       the user's latitude/longitude falls inside — return all polygon matches.</li>
   *   <li>Subdivision text-match: if no polygon matches (or no polygons available), delegate to
   *       {@link #filterByRegion} which checks {@code areaDesc} against the subdivision name.
   *       That method already falls back to all country-level warnings when there is no match.</li>
   * </ol>
   */
  List<MeteoAlarmWarning> filterForLocation(List<MeteoAlarmWarning> warnings, double latitude,
      double longitude, String subdivision) {
    if (warnings.isEmpty()) {
      return warnings;
    }
    // First pass: polygon-based point-in-polygon check
    List<MeteoAlarmWarning> polygonMatches = warnings.stream()
        .filter(w -> w.getPolygon() != null && !w.getPolygon().isBlank()
            && pointInPolygon(w.getPolygon(), latitude, longitude))
        .collect(java.util.stream.Collectors.toList());
    if (!polygonMatches.isEmpty()) {
      log.debug("Point-in-polygon matched {}/{} warning(s) for lat={} lon={}",
          polygonMatches.size(), warnings.size(), latitude, longitude);
      return polygonMatches;
    }
    log.debug("No polygon matches for lat={} lon={} — using subdivision text-match fallback",
        latitude, longitude);
    // Second pass: subdivision text-matching (with country-level fallback inside)
    return filterByRegion(warnings, subdivision);
  }

  /**
   * Checks whether a geographic point ({@code lat}, {@code lon}) lies inside a polygon defined
   * by a space-separated list of {@code "lat,lon"} coordinate pairs (CAP standard format).
   * Uses the ray-casting algorithm.
   */
  private boolean pointInPolygon(String polygon, double lat, double lon) {
    try {
      String[] points = polygon.trim().split("\\s+");
      int n = points.length;
      if (n < 3) {
        return false;
      }
      double[] lats = new double[n];
      double[] lons = new double[n];
      for (int i = 0; i < n; i++) {
        String[] parts = points[i].split(",");
        lats[i] = Double.parseDouble(parts[0].trim());
        lons[i] = Double.parseDouble(parts[1].trim());
      }
      // Ray-casting: count edge crossings for a horizontal ray eastward from (lat, lon)
      boolean inside = false;
      for (int i = 0, j = n - 1; i < n; j = i++) {
        if ((lons[i] > lon) != (lons[j] > lon)
            && lat < (lats[j] - lats[i]) * (lon - lons[i]) / (lons[j] - lons[i]) + lats[i]) {
          inside = !inside;
        }
      }
      return inside;
    } catch (Exception e) {
      log.debug("Failed to evaluate polygon for point-in-polygon check: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Fetches and parses MeteoGate warnings for the given country from the OGC API EDR.
   * Returns an empty list if the country is not supported, the API key is not configured,
   * or the request fails.
   */
  List<MeteoAlarmWarning> fetchWarnings(String countryCode) {
    if (countryCode == null) {
      return Collections.emptyList();
    }
    String upper = countryCode.toUpperCase();
    if (!SUPPORTED_COUNTRIES.contains(upper)) {
      log.debug("Country {} is not covered by MeteoGate — skipping alarm fetch", countryCode);
      return Collections.emptyList();
    }
    if (!apiKeyConfigured) {
      return Collections.emptyList();
    }
    // The datetime parameter is required by the API. The API enforces a maximum window of
    // less than 24 hours, so we use a 24-hour window to capture all currently active warnings.
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    String datetimeParam = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        + "/" + now.plusHours(24).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    log.info("Fetching MeteoGate warnings for country {}", countryCode);
    try {
      Map<String, Object> body = meteoGateClient.getWarnings(upper, datetimeParam);
      if (body == null) {
        log.warn("MeteoGate API returned null body for country {}", countryCode);
        return Collections.emptyList();
      }
      List<MeteoAlarmWarning> warnings = parseApiResponse(body);
      log.info("MeteoGate API for country {} returned {} warning(s)", countryCode,
          warnings.size());
      return warnings;
    } catch (Exception e) {
      log.warn("Failed to fetch MeteoGate warnings for country {}: {}", countryCode,
          e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Parses the MeteoGate OGC API EDR GeoJSON FeatureCollection response and returns a list of
   * warnings. Each feature's {@code properties} object carries CAP fields; the {@code geometry}
   * object carries the affected-area polygon in GeoJSON format ([lon, lat] order), which is
   * converted to the CAP text format ("lat,lon …") used by the point-in-polygon check.
   */
  @SuppressWarnings("unchecked")
  List<MeteoAlarmWarning> parseApiResponse(Map<String, Object> response) {
    List<Map<String, Object>> features;
    try {
      features = (List<Map<String, Object>>) response.get("features");
    } catch (ClassCastException e) {
      log.warn("MeteoGate API response has unexpected structure — 'features' is not a list: {}",
          e.getMessage());
      return Collections.emptyList();
    }
    if (features == null || features.isEmpty()) {
      log.info("MeteoGate API response contains no features");
      return Collections.emptyList();
    }
    log.info("MeteoGate API response contains {} feature(s)", features.size());
    List<MeteoAlarmWarning> warnings = new ArrayList<>();

    for (int i = 0; i < features.size(); i++) {
      Map<String, Object> feature = features.get(i);
      Map<String, Object> properties;
      try {
        properties = (Map<String, Object>) feature.get("properties");
      } catch (ClassCastException e) {
        log.warn("Feature[{}]: skipping — 'properties' has unexpected type: {}", i, e.getMessage());
        continue;
      }
      if (properties == null) {
        log.warn("Feature[{}]: skipping — no properties found", i);
        continue;
      }

      String awarenessLevel = getString(properties, "awareness_level");
      String awarenessType = parseAwarenessTypeName(getString(properties, "awareness_type"));
      String event = getString(properties, "event");
      String severity = getString(properties, "severity");
      String certainty = getString(properties, "certainty");
      String urgency = getString(properties, "urgency");
      String senderName = getString(properties, "senderName");
      String headline = getString(properties, "headline");
      String description = getString(properties, "description");
      String instruction = getString(properties, "instruction");
      String areaDesc = getString(properties, "areaDesc");
      OffsetDateTime onset = parseDateTime(properties, "onset", i, headline);
      OffsetDateTime expires = parseDateTime(properties, "expires", i, headline);
      OffsetDateTime effective = parseDateTime(properties, "effective", i, headline);

      // GeoJSON polygon: coordinates are [[[lon, lat], ...]] — convert to CAP text "lat,lon …"
      String polygon = extractGeoJsonPolygon(feature);

      log.debug("Feature[{}]: awarenessLevel='{}' awarenessType='{}' event='{}' severity='{}' "
              + "certainty='{}' urgency='{}' sender='{}' areaDesc='{}' "
              + "polygon={} onset={} expires={}",
          i, awarenessLevel, awarenessType, event, severity, certainty, urgency, senderName,
          areaDesc, polygon != null ? "(present)" : "(absent)", onset, expires);

      if (awarenessLevel == null || awarenessLevel.isBlank()) {
        log.warn("Feature[{}]: skipping — no awarenessLevel found (headline='{}')", i, headline);
        continue;
      }
      if (onset == null) {
        log.warn("Feature[{}]: onset is missing for warning '{}'", i,
            headline != null ? headline : awarenessLevel);
      }
      if (expires == null) {
        log.warn("Feature[{}]: expires is missing for warning '{}'", i,
            headline != null ? headline : awarenessLevel);
      }
      if (areaDesc == null) {
        log.warn("Feature[{}]: areaDesc is missing for warning '{}'", i,
            headline != null ? headline : awarenessLevel);
      }
      if (polygon == null) {
        log.debug("Feature[{}]: no polygon geometry — will fall back to subdivision text-match",
            i);
      }

      warnings.add(MeteoAlarmWarning.builder()
          .awarenessLevel(awarenessLevel)
          .awarenessType(awarenessType)
          .event(event)
          .severity(severity)
          .certainty(certainty)
          .urgency(urgency)
          .senderName(senderName)
          .headline(headline)
          .description(description)
          .instruction(instruction)
          .areaDesc(areaDesc)
          .polygon(polygon)
          .effective(effective)
          .onset(onset)
          .expires(expires)
          .build());
    }
    log.info("Parsed {} valid warning(s) from MeteoGate API response", warnings.size());
    return warnings;
  }

  /**
   * Returns the string value of a property key, trimmed; null when absent or empty.
   */
  private String getString(Map<String, Object> props, String key) {
    Object val = props.get(key);
    if (val == null) {
      return null;
    }
    String s = val.toString().trim();
    return s.isEmpty() ? null : s;
  }

  /**
   * Parses an ISO-8601 date-time string from the given property key.
   * Logs a debug message and returns null when the value is missing or cannot be parsed.
   */
  private OffsetDateTime parseDateTime(Map<String, Object> props, String key,
      int featureIndex, String context) {
    String val = getString(props, key);
    if (val == null) {
      return null;
    }
    try {
      return OffsetDateTime.parse(val);
    } catch (Exception e) {
      log.debug("Feature[{}]: could not parse {} '{}' — {}", featureIndex, key, val,
          e.getMessage());
      return null;
    }
  }

  /**
   * Extracts the GeoJSON polygon geometry from a feature and converts it to the CAP text format
   * used by {@link #pointInPolygon}: space-separated {@code "lat,lon"} pairs.
   * GeoJSON uses [lon, lat] coordinate order; CAP uses lat,lon order.
   * Returns null when no valid Polygon geometry is present or geometry is malformed.
   */
  @SuppressWarnings("unchecked")
  private String extractGeoJsonPolygon(Map<String, Object> feature) {
    try {
      Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
      if (geometry == null) {
        return null;
      }
      String type = getString(geometry, "type");
      if (!"Polygon".equals(type)) {
        return null;
      }
      List<List<List<Number>>> coords =
          (List<List<List<Number>>>) geometry.get("coordinates");
      if (coords == null || coords.isEmpty()) {
        return null;
      }
      List<List<Number>> ring = coords.get(0); // outer ring
      if (ring == null || ring.size() < 3) {
        return null;
      }
      StringBuilder sb = new StringBuilder();
      for (List<Number> point : ring) {
        if (point.size() < 2) {
          continue;
        }
        double lon = point.get(0).doubleValue();
        double lat = point.get(1).doubleValue();
        if (sb.length() > 0) {
          sb.append(' ');
        }
        sb.append(lat).append(',').append(lon);
      }
      String result = sb.toString().trim();
      return result.isEmpty() ? null : result;
    } catch (ClassCastException | NullPointerException e) {
      log.debug("Could not extract GeoJSON polygon from feature geometry: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Parses the human-readable type name from an awareness_type CAP value.
   * Strips the leading numeric code: "4; Thunderstorm" → "Thunderstorm".
   */
  private String parseAwarenessTypeName(String raw) {
    if (raw == null) {
      return null;
    }
    int semicolon = raw.indexOf(';');
    return semicolon >= 0 && semicolon < raw.length() - 1
        ? raw.substring(semicolon + 1).trim()
        : raw;
  }

  /**
   * Returns the highest active alarm level from the given warnings at the given moment.
   */
  WeatherAlarm resolveHighestActive(List<MeteoAlarmWarning> warnings, OffsetDateTime now) {
    WeatherAlarm highest = WeatherAlarm.GREEN;
    for (MeteoAlarmWarning warning : warnings) {
      if (isActive(warning, now)) {
        WeatherAlarm level = WeatherAlarm.fromAwarenessLevel(warning.getAwarenessLevel());
        if (level.ordinal() > highest.ordinal()) {
          highest = level;
        }
      }
    }
    return highest;
  }

  /**
   * Returns the highest alarm level overlapping the given date, or null when there is no alarm.
   */
  WeatherAlarm resolveAlarmForDay(List<MeteoAlarmWarning> warnings, LocalDate date,
      OffsetDateTime now) {
    WeatherAlarm highest = WeatherAlarm.GREEN;
    OffsetDateTime dayStart = date.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
    OffsetDateTime dayEnd = dayStart.plusDays(1);

    for (MeteoAlarmWarning warning : warnings) {
      boolean overlaps;
      if (warning.getOnset() == null || warning.getExpires() == null) {
        // No time range available: treat as active today only
        overlaps = date.equals(now.toLocalDate());
      } else {
        overlaps = warning.getOnset().isBefore(dayEnd) && warning.getExpires().isAfter(dayStart);
      }
      if (overlaps) {
        WeatherAlarm level = WeatherAlarm.fromAwarenessLevel(warning.getAwarenessLevel());
        if (level.ordinal() > highest.ordinal()) {
          highest = level;
        }
      }
    }
    return highest == WeatherAlarm.GREEN ? null : highest;
  }

  /**
   * Checks if a warning is currently active at the given moment.
   * When onset/expires are not available, the warning is assumed to be active
   * (MeteoGate API typically only includes currently active warnings).
   */
  boolean isActive(MeteoAlarmWarning warning, OffsetDateTime now) {
    if (warning.getOnset() == null || warning.getExpires() == null) {
      return true;
    }
    return !now.isBefore(warning.getOnset()) && !now.isAfter(warning.getExpires());
  }

}
