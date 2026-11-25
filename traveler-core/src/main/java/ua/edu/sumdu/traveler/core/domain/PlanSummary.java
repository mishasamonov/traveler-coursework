package ua.edu.sumdu.traveler.core.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Row of a plan list: the plan itself plus aggregated data about its route. */
public record PlanSummary(
    UUID id,
    String title,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal budget,
    String currency,
    boolean isPublic,
    UUID ownerId,
    String ownerName,
    long locationCount,
    BigDecimal plannedSpend,
    Instant updatedAt,
    PlanStatus status) {

  public Long days() {
    if (startDate == null || endDate == null) return null;
    return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
  }

  public boolean overBudget() {
    return budget != null && plannedSpend != null && plannedSpend.compareTo(budget) > 0;
  }
}
