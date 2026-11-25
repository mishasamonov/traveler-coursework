package ua.edu.sumdu.traveler.core.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Point of the route (table locations). visitOrder is the position in the route: 1, 2, 3... */
public record Location(
    UUID id,
    UUID travelPlanId,
    String name,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    int visitOrder,
    OffsetDateTime arrivalDate,
    OffsetDateTime departureDate,
    BigDecimal budget,
    String notes,
    int version,
    Instant createdAt) {

  public boolean hasCoordinates() {
    return latitude != null && longitude != null;
  }

  /** Planned stay in hours, if both times are known. */
  public Long stayHours() {
    if (arrivalDate == null || departureDate == null) return null;
    return Duration.between(arrivalDate, departureDate).toHours();
  }
}
