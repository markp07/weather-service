package nl.markpost.weather.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class CacheConfig implements CachingConfigurer {

  private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

  /**
   * Creates a Jackson {@link ObjectMapper} configured for Redis cache serialization.
   * Uses ISO-8601 date strings (not timestamps) and embeds class type information so
   * that generic collections ({@code List<Location>}, {@code List<MeteoAlarmWarning>}, etc.)
   * can be deserialized without requiring {@link java.io.Serializable}.
   */
  private static ObjectMapper cacheObjectMapper() {
    return JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
            ObjectMapper.DefaultTyping.EVERYTHING,
            JsonTypeInfo.As.PROPERTY)
        .build();
  }

  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
    GenericJackson2JsonRedisSerializer jsonSerializer =
        new GenericJackson2JsonRedisSerializer(cacheObjectMapper());

    RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

    // Calculate TTL until midnight
    LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
    LocalDateTime midnight = now.toLocalDate().atStartOfDay().plusDays(1);
    long secondsUntilMidnight = java.time.Duration.between(now, midnight).getSeconds();

    cacheConfigurations.put("weatherDaily",
        baseConfig.entryTtl(Duration.ofSeconds(secondsUntilMidnight)));

    // Calculate TTL until the next full hour
    LocalDateTime nextHour = now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
    long secondsUntilNextHour = java.time.Duration.between(now, nextHour).getSeconds();

    cacheConfigurations.put("weatherHourly",
        baseConfig.entryTtl(Duration.ofSeconds(secondsUntilNextHour)));

    cacheConfigurations.put("location",
        baseConfig.entryTtl(Duration.ofDays(365)));

    cacheConfigurations.put("searchLocations",
        baseConfig.entryTtl(Duration.ofDays(30)));

    cacheConfigurations.put("weatherAlarms",
        baseConfig.entryTtl(Duration.ofMinutes(30)));

    cacheConfigurations.put("weatherAlarmsAll",
        baseConfig.entryTtl(Duration.ofMinutes(30)));

    return RedisCacheManager.builder(redisConnectionFactory)
        .cacheDefaults(baseConfig)
        .withInitialCacheConfigurations(cacheConfigurations)
        .build();
  }

  /**
   * Clears all caches on application startup. This ensures stale entries from a previous
   * deployment (e.g. entries written with a different serializer or an older data model) are
   * removed before the application begins serving requests.
   */
  @Bean
  public ApplicationRunner clearCachesOnStartup(CacheManager cacheManager) {
    return args -> {
      log.info("Clearing all caches on startup: {}", cacheManager.getCacheNames());
      cacheManager.getCacheNames().forEach(name -> {
        Cache cache = cacheManager.getCache(name);
        if (cache != null) {
          cache.clear();
        }
      });
      log.info("All caches cleared");
    };
  }

  /**
   * Handles Redis cache errors gracefully. When a stale entry written with the old Java
   * serializer (or any other deserialization failure) is encountered on a cache lookup,
   * the bad entry is evicted and a cache-miss is returned so the application can
   * recompute and repopulate the value. Other error types are logged and swallowed to
   * prevent cache unavailability from propagating as user-visible errors.
   */
  @Override
  public CacheErrorHandler errorHandler() {
    return new CacheErrorHandler() {

      @Override
      public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache get error on cache '{}' for key '{}' — evicting and treating as miss: {}",
            cache.getName(), key, exception.getMessage());
        try {
          cache.evict(key);
        } catch (RuntimeException evictException) {
          log.warn("Failed to evict key '{}' from cache '{}': {}",
              key, cache.getName(), evictException.getMessage());
        }
      }

      @Override
      public void handleCachePutError(RuntimeException exception, Cache cache, Object key,
          Object value) {
        log.warn("Cache put error on cache '{}' for key '{}': {}",
            cache.getName(), key, exception.getMessage());
      }

      @Override
      public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache evict error on cache '{}' for key '{}': {}",
            cache.getName(), key, exception.getMessage());
      }

      @Override
      public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Cache clear error on cache '{}': {}", cache.getName(), exception.getMessage());
      }
    };
  }
}
