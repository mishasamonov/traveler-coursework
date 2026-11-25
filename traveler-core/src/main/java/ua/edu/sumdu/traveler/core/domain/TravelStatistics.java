package ua.edu.sumdu.traveler.core.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Figures of the administrator dashboard and of the "travel" Actuator endpoint. */
public record TravelStatistics(
    long users,
    long administrators,
    long blockedUsers,
    long plans,
    long publicPlans,
    long ownerlessPlans,
    long locations,
    double averageLocationsPerPlan,
    Map<String, BigDecimal> averageBudgetByCurrency,
    List<NamedCount> topPlaces,
    long plansWithOrderGaps) {

  /** A name with a number, for example a place and how many plans visit it. */
  public record NamedCount(String name, long count) {}
}
