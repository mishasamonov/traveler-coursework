package ua.edu.sumdu.traveler.web.config;

import java.util.List;
import java.util.Locale;
import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Additional Actuator beans. */
@Configuration
public class ActuatorConfig {

  /** Last 200 HTTP requests for /actuator/httpexchanges (kept in memory only). */
  @Bean
  public HttpExchangeRepository httpExchangeRepository() {
    InMemoryHttpExchangeRepository repository = new InMemoryHttpExchangeRepository();
    repository.setCapacity(200);
    return repository;
  }

  /**
   * /actuator/env and /actuator/configprops show values to the administrator, but secrets are
   * always masked.
   */
  @Bean
  public SanitizingFunction secretsSanitizer() {
    List<String> secretWords = List.of("password", "secret", "token", "credential", "key");
    return data -> {
      String key = data.getKey().toLowerCase(Locale.ROOT);
      boolean secret = secretWords.stream().anyMatch(key::contains)
          || (data.getValue() != null && String.valueOf(data.getValue()).contains("://")
              && String.valueOf(data.getValue()).contains("@"));
      return secret ? data.withValue(SanitizableData.SANITIZED_VALUE) : data;
    };
  }
}
