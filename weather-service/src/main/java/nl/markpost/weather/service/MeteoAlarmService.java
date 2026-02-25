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
   * Returns the highest active weather alarm level for the given country, or GREEN if no alarm
   * is active or the country is not covered by MeteoAlarm.
   *
   * @param countryCode ISO 3166-1 alpha-2 country code (e.g. "NL")
   * @return the highest active WeatherAlarm level
   */
  @Cacheable(value = "weatherAlarms", key = "#countryCode")
  public WeatherAlarm getHighestAlarm(String countryCode) {
    List<MeteoAlarmWarning> warnings = fetchWarnings(countryCode);
    return resolveHighestActive(warnings, OffsetDateTime.now(ZoneOffset.UTC));
  }

  /**
   * Returns a list of active warnings for each daily entry date, or null when no alarm is active
   * on a given day.
   *
   * @param countryCode ISO 3166-1 alpha-2 country code
   * @param dates       list of daily dates to check alarms for
   * @return list of WeatherAlarm levels (same size as dates), null entries where no alarm is active
   */
  @Cacheable(value = "weatherAlarms", key = "#countryCode + '-daily'")
  public List<WeatherAlarm> getDailyAlarms(String countryCode, List<LocalDate> dates) {
    List<MeteoAlarmWarning> warnings = fetchWarnings(countryCode);
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    List<WeatherAlarm> result = new ArrayList<>();
    for (LocalDate date : dates) {
      result.add(resolveAlarmForDay(warnings, date, now));
    }
    return result;
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
      return Collections.emptyList();
    }
    try {
      String xml = restTemplate.getForObject(BASE_URL + slug, String.class);
      if (xml == null || xml.isBlank()) {
        return Collections.emptyList();
      }
      return parseAtomFeed(xml);
    } catch (Exception e) {
      log.warn("Failed to fetch MeteoAlarm feed for country {}: {}", countryCode, e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Parses the MeteoAlarm Atom/CAP feed XML and returns a list of warnings.
   * Extracts awareness_level, onset and expires from embedded CAP data when available,
   * otherwise falls back to parsing the entry title for color keywords.
   */
  List<MeteoAlarmWarning> parseAtomFeed(String xml) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder db = factory.newDocumentBuilder();
      Document doc = db.parse(new InputSource(new StringReader(xml)));

      NodeList entries = doc.getElementsByTagNameNS(ATOM_NS, "entry");
      List<MeteoAlarmWarning> warnings = new ArrayList<>();

      for (int i = 0; i < entries.getLength(); i++) {
        Element entry = (Element) entries.item(i);
        String awarenessLevel = extractAwarenessLevel(entry);
        OffsetDateTime onset = extractOffsetDateTime(entry, "onset");
        OffsetDateTime expires = extractOffsetDateTime(entry, "expires");

        if (awarenessLevel != null && !awarenessLevel.isBlank()) {
          warnings.add(new MeteoAlarmWarning(awarenessLevel, onset, expires));
        }
      }
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
    // Try to find awareness_level in embedded CAP <parameter> elements
    NodeList params = entry.getElementsByTagNameNS(CAP_NS, "parameter");
    for (int j = 0; j < params.getLength(); j++) {
      Element param = (Element) params.item(j);
      NodeList names = param.getElementsByTagNameNS(CAP_NS, "valueName");
      NodeList values = param.getElementsByTagNameNS(CAP_NS, "value");
      if (names.getLength() > 0 && "awareness_level".equals(names.item(0).getTextContent())
          && values.getLength() > 0) {
        return values.item(0).getTextContent();
      }
    }
    // Fallback: use the entry <title> which often encodes the level
    NodeList titles = entry.getElementsByTagNameNS(ATOM_NS, "title");
    if (titles.getLength() > 0) {
      return titles.item(0).getTextContent();
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
  private boolean isActive(MeteoAlarmWarning warning, OffsetDateTime now) {
    if (warning.getOnset() == null || warning.getExpires() == null) {
      return true;
    }
    return !now.isBefore(warning.getOnset()) && !now.isAfter(warning.getExpires());
  }

}
