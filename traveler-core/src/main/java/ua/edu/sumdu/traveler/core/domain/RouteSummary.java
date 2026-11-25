package ua.edu.sumdu.traveler.core.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Analytics of a route, calculated from the locations: distances between neighbouring points,
 * planned expenses against the plan budget and consistency warnings.
 *
 * @param legDistancesKm distance from the previous location for every location (null for the
 *     first one or when coordinates are missing), in the same order as the route
 */
public record RouteSummary(
    int locationCount,
    BigDecimal totalDistanceKm,
    List<BigDecimal> legDistancesKm,
    BigDecimal plannedSpend,
    BigDecimal budget,
    BigDecimal remaining,
    Integer budgetUsagePercent,
    boolean overBudget,
    Long tripDays,
    int mappedLocations,
    List<String> warnings) {

  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }
}
