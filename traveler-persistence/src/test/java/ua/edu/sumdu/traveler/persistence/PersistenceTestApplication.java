package ua.edu.sumdu.traveler.persistence;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** Test context of the persistence module: the database traveler_test is rebuilt by Flyway. */
@SpringBootApplication
@Import(PersistenceConfig.class)
public class PersistenceTestApplication {

  @Bean
  FlywayMigrationStrategy cleanMigrate() {
    return flyway -> {
      flyway.clean();
      flyway.migrate();
    };
  }
}
