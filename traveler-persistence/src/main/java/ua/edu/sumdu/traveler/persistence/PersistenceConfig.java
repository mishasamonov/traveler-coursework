package ua.edu.sumdu.traveler.persistence;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Entry point of the data access layer: entities, Spring Data repositories and the adapters that
 * implement the ports of the core. The web module imports this configuration.
 */
@Configuration
@EntityScan("ua.edu.sumdu.traveler.persistence.entity")
@EnableJpaRepositories("ua.edu.sumdu.traveler.persistence.repository")
@ComponentScan("ua.edu.sumdu.traveler.persistence.adapter")
public class PersistenceConfig {}
