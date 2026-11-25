package ua.edu.sumdu.traveler.web.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToDoubleFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ua.edu.sumdu.traveler.core.domain.TravelStatistics;
import ua.edu.sumdu.traveler.core.service.TravelPlanService;

/**
 * Business gauges for /actuator/metrics and /actuator/prometheus (traveler.plans, ...). The
 * figures are refreshed by a scheduled task once a minute instead of querying the database on
 * every scrape.
 */
@Component
public class TravelStatisticsCache implements MeterBinder {

  private static final Logger log = LoggerFactory.getLogger(TravelStatisticsCache.class);

  private final TravelPlanService plans;
  private final AtomicReference<TravelStatistics> snapshot = new AtomicReference<>();

  public TravelStatisticsCache(TravelPlanService plans) {
    this.plans = plans;
  }

  @Scheduled(initialDelayString = "PT5S", fixedDelayString = "${traveler.metrics.refresh:PT60S}")
  public void refresh() {
    try {
      snapshot.set(plans.monitoringSnapshot());
      log.debug("Travel statistics refreshed: {}", snapshot.get());
    } catch (RuntimeException e) {
      log.warn("Travel statistics are not available: {}", e.getMessage());
    }
  }

  public Optional<TravelStatistics> current() {
    return Optional.ofNullable(snapshot.get());
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    gauge(registry, "traveler.plans", "public", "true", TravelStatistics::publicPlans,
        "Travel plans in the database");
    gauge(registry, "traveler.plans", "public", "false", s -> s.plans() - s.publicPlans(),
        "Travel plans in the database");
    gauge(registry, "traveler.locations", null, null, TravelStatistics::locations,
        "Locations of all routes");
    gauge(registry, "traveler.users", null, null, TravelStatistics::users, "User accounts");
  }

  private void gauge(MeterRegistry registry, String name, String tagKey, String tagValue,
      ToDoubleFunction<TravelStatistics> value, String description) {
    Gauge.Builder<AtomicReference<TravelStatistics>> builder = Gauge.builder(name, snapshot,
            ref -> ref.get() == null ? Double.NaN : value.applyAsDouble(ref.get()))
        .description(description);
    if (tagKey != null) builder.tag(tagKey, tagValue);
    builder.register(registry);
  }
}
