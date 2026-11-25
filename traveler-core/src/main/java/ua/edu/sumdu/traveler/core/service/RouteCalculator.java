package ua.edu.sumdu.traveler.core.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ua.edu.sumdu.traveler.core.domain.Location;
import ua.edu.sumdu.traveler.core.domain.RouteSummary;
import ua.edu.sumdu.traveler.core.domain.TravelPlan;

/**
 * Route analytics that the raw tables do not give directly: the length of the route by the
 * great-circle (haversine) formula, planned expenses against the budget and the consistency of
 * the order of visits with their times.
 */
public class RouteCalculator {

  /** Mean Earth radius (IUGG), km. */
  static final double EARTH_RADIUS_KM = 6371.0088;

  private final ZoneId zone;

  public RouteCalculator(ZoneId zone) {
    this.zone = zone;
  }

  /** Great-circle distance between two points in kilometres, rounded to 0.1 km. */
  public static BigDecimal distanceKm(
      BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
    double phi1 = Math.toRadians(lat1.doubleValue());
    double phi2 = Math.toRadians(lat2.doubleValue());
    double dPhi = phi2 - phi1;
    double dLambda = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
    double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
        + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return BigDecimal.valueOf(EARTH_RADIUS_KM * c).setScale(1, RoundingMode.HALF_UP);
  }

  public RouteSummary summarize(TravelPlan plan, List<Location> route) {
    List<BigDecimal> legs = new ArrayList<>(route.size());
    List<String> warnings = new ArrayList<>();
    BigDecimal total = BigDecimal.ZERO.setScale(1);
    BigDecimal spend = BigDecimal.ZERO.setScale(2);
    int mapped = 0;
    Location previous = null;
    for (Location location : route) {
      BigDecimal leg = null;
      if (location.hasCoordinates()) {
        mapped++;
        if (previous != null && previous.hasCoordinates()) {
          leg = distanceKm(previous.latitude(), previous.longitude(),
              location.latitude(), location.longitude());
          total = total.add(leg);
        }
      }
      legs.add(leg);
      if (location.budget() != null) spend = spend.add(location.budget());
      checkDates(plan, location, warnings);
      if (previous != null && previous.departureDate() != null && location.arrivalDate() != null
          && location.arrivalDate().isBefore(previous.departureDate())) {
        warnings.add("«" + location.name() + "»: прибуття раніше, ніж відбуття з попередньої "
            + "локації «" + previous.name() + "». Перевірте порядок маршруту.");
      }
      previous = location;
    }

    BigDecimal budget = plan.budget();
    BigDecimal remaining = budget == null ? null : budget.subtract(spend);
    Integer usage = null;
    if (budget != null && budget.signum() > 0) {
      // Capped so that a tiny budget with large expenses still fits an int.
      usage = spend.multiply(BigDecimal.valueOf(100))
          .divide(budget, 0, RoundingMode.HALF_UP).min(BigDecimal.valueOf(9999)).intValue();
    }
    boolean over = remaining != null && remaining.signum() < 0;
    if (over) {
      warnings.add("Заплановані витрати на локації перевищують бюджет подорожі на "
          + remaining.negate().toPlainString() + " " + plan.currency() + ".");
    }
    int unmapped = route.size() - mapped;
    if (unmapped > 0) {
      warnings.add("Локацій без координат: " + unmapped
          + ". Вони не показані на карті й не враховані у довжині маршруту.");
    }
    return new RouteSummary(route.size(), total, Collections.unmodifiableList(legs), spend, budget,
        remaining, usage, over, plan.days(), mapped, List.copyOf(warnings));
  }

  private void checkDates(TravelPlan plan, Location location, List<String> warnings) {
    if (outside(plan, location.arrivalDate() == null ? null
        : location.arrivalDate().atZoneSameInstant(zone).toLocalDate())
        || outside(plan, location.departureDate() == null ? null
        : location.departureDate().atZoneSameInstant(zone).toLocalDate())) {
      warnings.add("«" + location.name() + "»: час відвідування виходить за межі дат подорожі.");
    }
  }

  private static boolean outside(TravelPlan plan, LocalDate day) {
    if (day == null) return false;
    return (plan.startDate() != null && day.isBefore(plan.startDate()))
        || (plan.endDate() != null && day.isAfter(plan.endDate()));
  }
}
