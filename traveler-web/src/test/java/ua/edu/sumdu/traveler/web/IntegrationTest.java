package ua.edu.sumdu.traveler.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Full application (all three layers) with MockMvc on the database traveler_test, which Flyway
 * cleans and migrates once per test run (schema V1-V2 + demo data V3).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
// Real meter registries (Prometheus) as in the application; tests disable them by default.
@AutoConfigureObservability(tracing = false)
@ActiveProfiles("test")
@Import(IntegrationTest.CleanDatabase.class)
public @interface IntegrationTest {

  UUID CARPATHIANS = UUID.fromString("f373cf43-3e57-5972-b98a-c6d350432837"); // Олена, public
  UUID LVIV = UUID.fromString("a0427357-7c1d-531d-8a9f-7d1655d1c966");        // Олена, public
  UUID ITALY = UUID.fromString("aa2b27aa-cc01-5e22-adfb-2280c3d38614");       // Олена, private
  UUID ODESA = UUID.fromString("dcc7a926-b4c0-59a5-a439-90c9ec4491f8");       // Андрій, public
  String OLENA = "olena@traveler.test";
  String ANDRII = "andrii@traveler.test";
  String ADMIN = "admin@traveler.test";

  @TestConfiguration
  class CleanDatabase {
    @Bean
    FlywayMigrationStrategy cleanMigrate() {
      return flyway -> {
        flyway.clean();
        flyway.migrate();
      };
    }
  }
}
