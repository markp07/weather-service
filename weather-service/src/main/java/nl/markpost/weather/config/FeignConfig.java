package nl.markpost.weather.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Eagerly initialises {@link FeignHttpMessageConverters} to prevent a race condition in Spring
 * Cloud OpenFeign where concurrent threads calling a {@code @Cacheable} Feign client method before
 * the class is lazily initialised receive an empty converter list, causing Spring's
 * {@code SpringDecoder} to throw:
 *
 * <pre>feign.codec.DecodeException: 'messageConverters' must not be empty</pre>
 *
 * <p>The fix forces {@link FeignHttpMessageConverters#getConverters()} at bean-creation time so
 * that the converter list is fully populated before any Feign call is made.
 *
 * @see <a href="https://github.com/spring-cloud/spring-cloud-openfeign/issues/933">
 *     spring-cloud-openfeign#933</a>
 */
@Configuration
public class FeignConfig {

  @Bean
  @ConditionalOnMissingBean
  public FeignHttpMessageConverters feignHttpMessageConverters(
      ObjectProvider<ClientHttpMessageConvertersCustomizer> messageConvertersCustomizers,
      ObjectProvider<HttpMessageConverterCustomizer> feignConverterCustomizers) {
    var converters = new FeignHttpMessageConverters(messageConvertersCustomizers, feignConverterCustomizers);
    // Eagerly trigger lazy initialisation so concurrent threads never observe an empty list
    converters.getConverters();
    return converters;
  }
}
