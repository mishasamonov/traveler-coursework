package ua.edu.sumdu.traveler.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import ua.edu.sumdu.traveler.persistence.PersistenceConfig;

/**
 * Traveler — information system for planning trips over the TravelerAPI database.
 * Layers: traveler-web (presentation) -> traveler-core (business logic) <- traveler-persistence
 * (data access, imported here).
 */
@SpringBootApplication
@Import(PersistenceConfig.class)
@EnableScheduling
public class TravelerApplication {

  public static void main(String[] args) {
    SpringApplication.run(TravelerApplication.class, args);
  }
}
