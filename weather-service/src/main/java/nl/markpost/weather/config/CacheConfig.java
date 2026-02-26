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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
public class CacheConfig {

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

    return RedisCacheManager.builder(redisConnectionFactory)
        .cacheDefaults(baseConfig)
        .withInitialCacheConfigurations(cacheConfigurations)
        .build();
  }
}
