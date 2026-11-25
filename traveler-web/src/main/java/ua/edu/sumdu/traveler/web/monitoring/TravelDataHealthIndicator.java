package ua.edu.sumdu.traveler.web.monitoring;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import ua.edu.sumdu.traveler.core.domain.TravelStatistics;
import ua.edu.sumdu.traveler.core.service.TravelPlanService;

/**
 * Component "travelData" of /actuator/health: the domain data can be read and the routes are
 * consistent. Gaps in visit_order (possible after deletions through the REST API of the labs)
 * are reported as a detail, not as a failure: the application keeps working with them.
 */
@Component("travelData")
public class TravelDataHealthIndicator implements HealthIndicator {

  private final TravelPlanService plans;

  public TravelDataHealthIndicator(TravelPlanService plans) {
    this.plans = plans;
  }

  @Override
  public Health health() {
    TravelStatistics s = plans.monitoringSnapshot();
    return Health.up()
        .withDetail("plans", s.plans())
        .withDetail("publicPlans", s.publicPlans())
        .withDetail("locations", s.locations())
        .withDetail("ownerlessPlans", s.ownerlessPlans())
        .withDetail("plansWithOrderGaps", s.plansWithOrderGaps())
        .withDetail("routeOrderConsistent", s.plansWithOrderGaps() == 0)
        .build();
  }
}
