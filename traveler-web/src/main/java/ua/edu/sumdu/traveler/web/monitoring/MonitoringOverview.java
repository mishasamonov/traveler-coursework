package ua.edu.sumdu.traveler.web.monitoring;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint.MetricDescriptor;
import org.springframework.boot.actuate.metrics.MetricsEndpoint.Sample;
import io.micrometer.core.instrument.Statistic;
import org.springframework.stereotype.Component;

/**
 * Reads the Actuator endpoints in-process (the same data as /actuator/health and
 * /actuator/metrics) and turns them into the "Стан системи" panel of the admin dashboard.
 */
@Component
public class MonitoringOverview {

  private final HealthEndpoint health;
  private final MetricsEndpoint metrics;

  public MonitoringOverview(HealthEndpoint health, MetricsEndpoint metrics) {
    this.health = health;
    this.metrics = metrics;
  }

  /** Figures of the panel; null means that the metric has no data yet. */
  public record Snapshot(
      String status,
      Map<String, String> components,
      Double uptimeMinutes,
      Double heapUsedMb,
      Double heapMaxMb,
      Double httpRequests,
      Double httpMeanMs,
      Double httpErrors,
      Double dbActive,
      Double dbIdle,
      Double dbMax,
      Double serviceCalls,
      Double serviceMeanMs,
      Double threads,
      Double cpuPercent) {}

  public Snapshot snapshot() {
    HealthComponent root = health.health();
    Map<String, String> components = new LinkedHashMap<>();
    if (root instanceof CompositeHealth composite) {
      composite.getComponents().forEach((name, c) -> components.put(name, c.getStatus().getCode()));
    }
    Double uptime = value("process.uptime", List.of(), Statistic.VALUE);
    Double used = value("jvm.memory.used", List.of("area:heap"), Statistic.VALUE);
    Double max = value("jvm.memory.max", List.of("area:heap"), Statistic.VALUE);
    Double requests = value("http.server.requests", List.of(), Statistic.COUNT);
    Double requestTime = value("http.server.requests", List.of(), Statistic.TOTAL_TIME);
    Double errors = value("http.server.requests", List.of("outcome:SERVER_ERROR"), Statistic.COUNT);
    Double calls = value("traveler.service", List.of(), Statistic.COUNT);
    Double callTime = value("traveler.service", List.of(), Statistic.TOTAL_TIME);
    Double cpu = value("process.cpu.usage", List.of(), Statistic.VALUE);
    return new Snapshot(root.getStatus().getCode(), components,
        uptime == null ? null : uptime / 60,
        used == null ? null : used / 1024 / 1024,
        max == null || max < 0 ? null : max / 1024 / 1024,
        requests, mean(requestTime, requests), errors == null ? 0.0 : errors,
        value("hikaricp.connections.active", List.of(), Statistic.VALUE),
        value("hikaricp.connections.idle", List.of(), Statistic.VALUE),
        value("hikaricp.connections.max", List.of(), Statistic.VALUE),
        calls, mean(callTime, calls),
        value("jvm.threads.live", List.of(), Statistic.VALUE),
        cpu == null ? null : cpu * 100);
  }

  /** TOTAL_TIME of timers is reported in seconds. */
  private static Double mean(Double totalSeconds, Double count) {
    if (totalSeconds == null || count == null || count == 0) return null;
    return totalSeconds * 1000 / count;
  }

  private Double value(String name, List<String> tags, Statistic statistic) {
    MetricDescriptor descriptor = metrics.metric(name, tags);
    if (descriptor == null) return null;
    for (Sample sample : descriptor.getMeasurements()) {
      if (sample.getStatistic() == statistic) return sample.getValue();
    }
    return null;
  }
}
