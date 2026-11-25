package ua.edu.sumdu.traveler.web.monitoring;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;
import ua.edu.sumdu.traveler.core.domain.TravelStatistics;
import ua.edu.sumdu.traveler.core.service.TravelPlanService;

/** Own Actuator endpoint GET /actuator/travel: up-to-date statistics of the subject area. */
@Component
@Endpoint(id = "travel")
public class TravelStatisticsEndpoint {

  private final TravelPlanService plans;

  public TravelStatisticsEndpoint(TravelPlanService plans) {
    this.plans = plans;
  }

  @ReadOperation
  public TravelStatistics statistics() {
    return plans.monitoringSnapshot();
  }
}
