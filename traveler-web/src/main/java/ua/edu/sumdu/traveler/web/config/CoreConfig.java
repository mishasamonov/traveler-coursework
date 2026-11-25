package ua.edu.sumdu.traveler.web.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ua.edu.sumdu.traveler.core.port.LocationPort;
import ua.edu.sumdu.traveler.core.port.PasswordHasher;
import ua.edu.sumdu.traveler.core.port.TravelPlanPort;
import ua.edu.sumdu.traveler.core.port.UserPort;
import ua.edu.sumdu.traveler.core.service.LocationService;
import ua.edu.sumdu.traveler.core.service.RouteCalculator;
import ua.edu.sumdu.traveler.core.service.TravelPlanService;
import ua.edu.sumdu.traveler.core.service.UserService;

/**
 * Wires the business layer: the core services are plain classes, Spring creates them here and
 * passes the adapters of the persistence module as their ports. @Transactional on the services
 * makes Spring wrap them into transactional proxies.
 */
@Configuration
public class CoreConfig {

  @Bean
  public ZoneId travelerZone(@Value("${traveler.zone:Europe/Kyiv}") String zone) {
    return ZoneId.of(zone);
  }

  @Bean
  public Clock clock(ZoneId travelerZone) {
    return Clock.system(travelerZone);
  }

  @Bean
  public RouteCalculator routeCalculator(ZoneId travelerZone) {
    return new RouteCalculator(travelerZone);
  }

  @Bean
  public TravelPlanService travelPlanService(TravelPlanPort plans, LocationPort locations,
      UserPort users, RouteCalculator calculator, Clock clock) {
    return new TravelPlanService(plans, locations, users, calculator, clock);
  }

  @Bean
  public LocationService locationService(TravelPlanPort plans, LocationPort locations,
      ZoneId travelerZone) {
    return new LocationService(plans, locations, travelerZone);
  }

  @Bean
  public UserService userService(UserPort users, PasswordHasher passwordHasher) {
    return new UserService(users, passwordHasher);
  }
}
