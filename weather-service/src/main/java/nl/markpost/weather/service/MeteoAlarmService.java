package nl.markpost.weather.service;

import java.io.StringReader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import nl.markpost.weather.model.MeteoAlarmWarning;
import nl.markpost.weather.model.WeatherAlarm;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Service for fetching official weather alarms from MeteoAlarm.
 * MeteoAlarm is backed by EUMETNET and the national meteorological services of 35+ European
 * countries, including KNMI (Netherlands), DWD (Germany), Météo-France, and others.
 * Feed URL: https://feeds.meteoalarm.org/feeds/meteoalarm-legacy-atom-{country}
 */
@Slf4j
@Service
public class MeteoAlarmService {

  private static final String BASE_URL =
      "https://feeds.meteoalarm.org/feeds/meteoalarm-legacy-atom-";

  private static final String ATOM_NS = "http://www.w3.org/2005/Atom";
  private static final String CAP_NS = "urn:oasis:names:tc:emergency:cap:1.2";

  /**
   * Mapping from ISO 3166-1 alpha-2 country code to MeteoAlarm country slug.
   */
  private static final Map<String, String> COUNTRY_SLUGS = Map.ofEntries(
      Map.entry("AT", "austria"),
      Map.entry("BE", "belgium"),
      Map.entry("BA", "bosnia-herzegovina"),
      Map.entry("BG", "bulgaria"),
      Map.entry("HR", "croatia"),
      Map.entry("CY", "cyprus"),
      Map.entry("CZ", "czechia"),
      Map.entry("DK", "denmark"),
      Map.entry("EE", "estonia"),
      Map.entry("FI", "finland"),
      Map.entry("FR", "france"),
      Map.entry("DE", "germany"),
      Map.entry("GR", "greece"),
      Map.entry("HU", "hungary"),
      Map.entry("IS", "iceland"),
      Map.entry("IE", "ireland"),
      Map.entry("IL", "israel"),
      Map.entry("IT", "italy"),
      Map.entry("LV", "latvia"),
      Map.entry("LT", "lithuania"),
      Map.entry("LU", "luxembourg"),
      Map.entry("MT", "malta"),
      Map.entry("MD", "moldova"),
      Map.entry("ME", "montenegro"),
      Map.entry("NL", "netherlands"),
      Map.entry("MK", "republic-of-north-macedonia"),
      Map.entry("NO", "norway"),
      Map.entry("PL", "poland"),
      Map.entry("PT", "portugal"),
      Map.entry("RO", "romania"),
      Map.entry("RS", "serbia"),
      Map.entry("SK", "slovakia"),
      Map.entry("SI", "slovenia"),
      Map.entry("ES", "spain"),
      Map.entry("SE", "sweden"),
      Map.entry("CH", "switzerland"),
      Map.entry("UA", "ukraine"),
      Map.entry("GB", "united-kingdom")
  );

  private final RestTemplate restTemplate;

  public MeteoAlarmService(RestTemplateBuilder builder) {
    this.restTemplate = builder
        .connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(10))
        .build();
  }

  /**
   * Returns the highest active weather alarm level for the given location, or GREEN if no alarm
   * is active or the country is not covered by MeteoAlarm.
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
   * Returns an empty list when the country is not covered by MeteoAlarm or no alarms are active.
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
   *   <li>Point-in-polygon: if any warnings carry a CAP {@code <polygon>}, check whether
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
   * Fetches and parses MeteoAlarm warnings for the given country.
   * Returns an empty list if the country is not covered or the feed is unavailable.
   */
  List<MeteoAlarmWarning> fetchWarnings(String countryCode) {
    if (countryCode == null) {
      return Collections.emptyList();
    }
    String slug = COUNTRY_SLUGS.get(countryCode.toUpperCase());
    if (slug == null) {
      log.debug("Country {} is not covered by MeteoAlarm — no slug found", countryCode);
      return Collections.emptyList();
    }
    String url = BASE_URL + slug;
    log.info("Fetching MeteoAlarm feed for country {} from {}", countryCode, url);
    try {
      String xml = restTemplate.getForObject(url, String.class);
      if (xml == null || xml.isBlank()) {
        log.warn("MeteoAlarm feed for country {} returned an empty response", countryCode);
        return Collections.emptyList();
      }
      List<MeteoAlarmWarning> warnings = parseAtomFeed(xml);
      log.info("MeteoAlarm feed for country {} returned {} warning(s)", countryCode,
          warnings.size());
      return warnings;
    } catch (Exception e) {
      log.warn("Failed to fetch MeteoAlarm feed for country {}: {}", countryCode, e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Parses the MeteoAlarm Atom/CAP feed XML and returns a list of warnings.
   * Extracts awareness_level, awareness_type, headline, description, areaDesc, onset and expires
   * from embedded CAP data when available, otherwise falls back to parsing the entry title.
   */
  List<MeteoAlarmWarning> parseAtomFeed(String xml) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder db = factory.newDocumentBuilder();
      Document doc = db.parse(new InputSource(new StringReader(xml)));

      NodeList entries = doc.getElementsByTagNameNS(ATOM_NS, "entry");
      log.info("MeteoAlarm feed contains {} entry(ies)", entries.getLength());
      List<MeteoAlarmWarning> warnings = new ArrayList<>();

      for (int i = 0; i < entries.getLength(); i++) {
        Element entry = (Element) entries.item(i);
        String awarenessLevel = extractAwarenessLevel(entry);
        String awarenessType = parseAwarenessTypeName(extractCapParam(entry, "awareness_type"));
        String event = extractCapText(entry, "event");
        String severity = extractCapText(entry, "severity");
        String certainty = extractCapText(entry, "certainty");
        String urgency = extractCapText(entry, "urgency");
        String senderName = extractCapText(entry, "senderName");
        String headline = extractCapText(entry, "headline");
        String description = extractCapText(entry, "description");
        String areaDesc = extractCapText(entry, "areaDesc");
        String polygon = extractCapText(entry, "polygon");
        OffsetDateTime onset = extractOffsetDateTime(entry, "onset");
        OffsetDateTime expires = extractOffsetDateTime(entry, "expires");

        log.debug("Entry[{}]: awarenessLevel='{}' awarenessType='{}' event='{}' severity='{}' "
                + "certainty='{}' urgency='{}' sender='{}' areaDesc='{}' "
                + "polygon={} onset={} expires={}",
            i, awarenessLevel, awarenessType, event, severity, certainty, urgency, senderName,
            areaDesc, polygon != null ? "(present)" : "(absent)", onset, expires);

        if (awarenessLevel == null || awarenessLevel.isBlank()) {
          log.warn("Entry[{}]: skipping — no awarenessLevel found (headline='{}')", i, headline);
          continue;
        }
        if (onset == null) {
          log.warn("Entry[{}]: onset is missing for warning '{}'", i,
              headline != null ? headline : awarenessLevel);
        }
        if (expires == null) {
          log.warn("Entry[{}]: expires is missing for warning '{}'", i,
              headline != null ? headline : awarenessLevel);
        }
        if (areaDesc == null) {
          log.warn("Entry[{}]: areaDesc is missing for warning '{}'", i,
              headline != null ? headline : awarenessLevel);
        }
        if (polygon == null) {
          log.debug("Entry[{}]: no polygon geometry — will fall back to subdivision text-match",
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
            .areaDesc(areaDesc)
            .polygon(polygon)
            .onset(onset)
            .expires(expires)
            .build());
      }
      log.info("Parsed {} valid warning(s) from MeteoAlarm feed", warnings.size());
      return warnings;
    } catch (Exception e) {
      log.warn("Failed to parse MeteoAlarm Atom feed: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Extracts the awareness_level from the CAP parameter section of an Atom entry,
   * falling back to the entry title if the CAP parameter is not found.
   */
  private String extractAwarenessLevel(Element entry) {
    String value = extractCapParam(entry, "awareness_level");
    if (value != null) {
      return value;
    }
    // Fallback: use the entry <title> which often encodes the level
    NodeList titles = entry.getElementsByTagNameNS(ATOM_NS, "title");
    if (titles.getLength() > 0) {
      return titles.item(0).getTextContent();
    }
    return null;
  }

  /**
   * Extracts the value of a named CAP parameter (inside &lt;cap:parameter&gt;) from an entry.
   * Returns null when the parameter is not present.
   */
  private String extractCapParam(Element entry, String paramName) {
    NodeList params = entry.getElementsByTagNameNS(CAP_NS, "parameter");
    for (int j = 0; j < params.getLength(); j++) {
      Element param = (Element) params.item(j);
      NodeList names = param.getElementsByTagNameNS(CAP_NS, "valueName");
      NodeList values = param.getElementsByTagNameNS(CAP_NS, "value");
      if (names.getLength() > 0 && paramName.equals(names.item(0).getTextContent())
          && values.getLength() > 0) {
        return values.item(0).getTextContent().trim();
      }
    }
    return null;
  }

  /**
   * Parses the human-readable type name from an awareness_type CAP value.
   * Strips the leading numeric code: "4; Thunderstorm" → "Thunderstorm".
   */
  private String parseAwarenessTypeName(String raw) {
    if (raw == null) return null;
    int semicolon = raw.indexOf(';');
    return semicolon >= 0 && semicolon < raw.length() - 1
        ? raw.substring(semicolon + 1).trim()
        : raw;
  }

  /**
   * Extracts the text content of a CAP element by local name from an Atom entry.
   */
  private String extractCapText(Element entry, String capElementName) {
    NodeList nodes = entry.getElementsByTagNameNS(CAP_NS, capElementName);
    if (nodes.getLength() > 0) {
      String text = nodes.item(0).getTextContent().trim();
      return text.isEmpty() ? null : text;
    }
    return null;
  }

  /**
   * Extracts an OffsetDateTime from a CAP element name within an Atom entry.
   */
  private OffsetDateTime extractOffsetDateTime(Element entry, String capElementName) {
    NodeList nodes = entry.getElementsByTagNameNS(CAP_NS, capElementName);
    if (nodes.getLength() > 0) {
      try {
        return OffsetDateTime.parse(nodes.item(0).getTextContent().trim());
      } catch (Exception e) {
        log.debug("Could not parse {} date: {}", capElementName, e.getMessage());
      }
    }
    return null;
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
   * (MeteoAlarm feeds typically only include currently active warnings).
   */
  boolean isActive(MeteoAlarmWarning warning, OffsetDateTime now) {
    if (warning.getOnset() == null || warning.getExpires() == null) {
      return true;
    }
    return !now.isBefore(warning.getOnset()) && !now.isAfter(warning.getExpires());
  }

}
