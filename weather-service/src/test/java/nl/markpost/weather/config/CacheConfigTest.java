package nl.markpost.weather.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationListener;

@ExtendWith(MockitoExtension.class)
class CacheConfigTest {

  private final CacheConfig cacheConfig = new CacheConfig();

  @Test
  @DisplayName("Should clear all caches when application is ready")
  void cacheFlushOnStartup_clearsAllCaches() {
    CacheManager cacheManager = mock(CacheManager.class);
    Cache weatherDaily = mock(Cache.class);
    Cache weatherHourly = mock(Cache.class);
    Cache location = mock(Cache.class);
    Cache searchLocations = mock(Cache.class);

    when(cacheManager.getCacheNames())
        .thenReturn(List.of("weatherDaily", "weatherHourly", "location", "searchLocations"));
    when(cacheManager.getCache("weatherDaily")).thenReturn(weatherDaily);
    when(cacheManager.getCache("weatherHourly")).thenReturn(weatherHourly);
    when(cacheManager.getCache("location")).thenReturn(location);
    when(cacheManager.getCache("searchLocations")).thenReturn(searchLocations);

    ApplicationListener<ApplicationReadyEvent> listener =
        cacheConfig.cacheFlushOnStartup(cacheManager);
    listener.onApplicationEvent(mock(ApplicationReadyEvent.class));

    verify(weatherDaily).clear();
    verify(weatherHourly).clear();
    verify(location).clear();
    verify(searchLocations).clear();
  }

  @Test
  @DisplayName("Should skip null cache entries gracefully")
  void cacheFlushOnStartup_skipsNullCache() {
    CacheManager cacheManager = mock(CacheManager.class);

    when(cacheManager.getCacheNames()).thenReturn(List.of("unknown"));
    when(cacheManager.getCache("unknown")).thenReturn(null);

    ApplicationListener<ApplicationReadyEvent> listener =
        cacheConfig.cacheFlushOnStartup(cacheManager);
    // Should not throw
    listener.onApplicationEvent(mock(ApplicationReadyEvent.class));
  }

  @Test
  @DisplayName("Should do nothing when there are no caches")
  void cacheFlushOnStartup_noCaches() {
    CacheManager cacheManager = mock(CacheManager.class);
    Cache anyCache = mock(Cache.class);

    when(cacheManager.getCacheNames()).thenReturn(List.of());

    ApplicationListener<ApplicationReadyEvent> listener =
        cacheConfig.cacheFlushOnStartup(cacheManager);
    listener.onApplicationEvent(mock(ApplicationReadyEvent.class));

    verify(anyCache, never()).clear();
  }
}
