package ua.edu.sumdu.traveler.web.monitoring;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/** Adds the "travel" section to /actuator/info: what the instance serves (no personal data). */
@Component
public class TravelInfoContributor implements InfoContributor {

  private final TravelStatisticsCache cache;

  public TravelInfoContributor(TravelStatisticsCache cache) {
    this.cache = cache;
  }

  @Override
  public void contribute(Info.Builder builder) {
    Map<String, Object> travel = new LinkedHashMap<>();
    travel.put("database", "TravelerAPI schema (laboratory works «Розподілені сховища даних»)");
    cache.current().ifPresent(s -> {
      travel.put("publicPlans", s.publicPlans());
      travel.put("plans", s.plans());
      travel.put("locations", s.locations());
    });
    builder.withDetail("travel", travel);
  }
}
