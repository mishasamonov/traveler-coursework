package ua.edu.sumdu.traveler.core.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Travel plan (table travel_plans of the TravelerAPI database). ownerId is null for plans created
 * through the REST API of the laboratory works, which has no users.
 */
public record TravelPlan(
    UUID id,
    UUID ownerId,
    String title,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal budget,
    String currency,
    boolean isPublic,
    int version,
    Instant createdAt,
    Instant updatedAt) {

  public PlanStatus status(LocalDate today) {
    return PlanStatus.of(startDate, endDate, today);
  }

  /** Number of calendar days including the first and the last one; null without both dates. */
  public Long days() {
    if (startDate == null || endDate == null) return null;
    return ChronoUnit.DAYS.between(startDate, endDate) + 1;
  }
}
