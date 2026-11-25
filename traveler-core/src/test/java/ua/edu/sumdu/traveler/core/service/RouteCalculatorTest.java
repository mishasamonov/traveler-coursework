package ua.edu.sumdu.traveler.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.sumdu.traveler.core.domain.Location;
import ua.edu.sumdu.traveler.core.domain.PlanStatus;
import ua.edu.sumdu.traveler.core.domain.RouteSummary;
import ua.edu.sumdu.traveler.core.domain.TravelPlan;

class RouteCalculatorTest {

  private final RouteCalculator calculator = new RouteCalculator(ZoneId.of("Europe/Kyiv"));

  @Test
  @DisplayName("Відстань Київ — Львів за формулою гаверсинуса ≈ 469 км")
  void haversineDistance() {
    BigDecimal km = RouteCalculator.distanceKm(new BigDecimal("50.4501"), new BigDecimal("30.5234"),
        new BigDecimal("49.8397"), new BigDecimal("24.0297"));
    assertThat(km).isBetween(new BigDecimal("467"), new BigDecimal("471"));
    assertThat(RouteCalculator.distanceKm(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
        BigDecimal.ONE)).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName("Підсумок: довжина маршруту, витрати, відсоток бюджету, тривалість")
  void summary() {
    TravelPlan plan = plan(new BigDecimal("1000.00"));
    List<Location> route = List.of(
        location(1, "50.4501", "30.5234", "100.00", null, null),
        location(2, "49.8397", "24.0297", "250.50", null, null));
    RouteSummary s = calculator.summarize(plan, route);
    assertThat(s.locationCount()).isEqualTo(2);
    assertThat(s.legDistancesKm().get(0)).isNull();
    assertThat(s.legDistancesKm().get(1)).isEqualByComparingTo(s.totalDistanceKm());
    assertThat(s.plannedSpend()).isEqualByComparingTo("350.50");
    assertThat(s.remaining()).isEqualByComparingTo("649.50");
    assertThat(s.budgetUsagePercent()).isEqualTo(35);
    assertThat(s.overBudget()).isFalse();
    assertThat(s.tripDays()).isEqualTo(3);
    assertThat(s.warnings()).isEmpty();
  }

  @Test
  @DisplayName("Попередження: перевищення бюджету, локація без координат, порушення хронології")
  void warnings() {
    TravelPlan plan = plan(new BigDecimal("100.00"));
    OffsetDateTime day1 = OffsetDateTime.of(2027, 5, 1, 18, 0, 0, 0, ZoneOffset.ofHours(3));
    List<Location> route = List.of(
        location(1, "50.45", "30.52", "80.00", day1, day1.plusHours(2)),
        location(2, null, null, "40.00", day1.plusHours(1), null));
    RouteSummary s = calculator.summarize(plan, route);
    assertThat(s.overBudget()).isTrue();
    assertThat(s.mappedLocations()).isEqualTo(1);
    assertThat(s.warnings()).hasSize(3)
        .anyMatch(w -> w.contains("перевищують бюджет"))
        .anyMatch(w -> w.contains("без координат"))
        .anyMatch(w -> w.contains("прибуття раніше"));
  }

  @Test
  @DisplayName("Статус подорожі визначається датами відносно сьогодні")
  void status() {
    LocalDate today = LocalDate.of(2026, 11, 25);
    assertThat(PlanStatus.of(null, null, today)).isEqualTo(PlanStatus.DRAFT);
    assertThat(PlanStatus.of(today.plusDays(1), today.plusDays(3), today)).isEqualTo(PlanStatus.UPCOMING);
    assertThat(PlanStatus.of(today.minusDays(1), today.plusDays(1), today)).isEqualTo(PlanStatus.IN_PROGRESS);
    assertThat(PlanStatus.of(today, null, today)).isEqualTo(PlanStatus.IN_PROGRESS);
    assertThat(PlanStatus.of(today.minusDays(5), today.minusDays(1), today)).isEqualTo(PlanStatus.COMPLETED);
  }

  private static TravelPlan plan(BigDecimal budget) {
    return new TravelPlan(UUID.randomUUID(), null, "Тест", null, LocalDate.of(2027, 5, 1),
        LocalDate.of(2027, 5, 3), budget, "UAH", true, 1, null, null);
  }

  private static Location location(int order, String lat, String lon, String budget,
      OffsetDateTime arrival, OffsetDateTime departure) {
    return new Location(UUID.randomUUID(), UUID.randomUUID(), "Точка " + order, null,
        lat == null ? null : new BigDecimal(lat), lon == null ? null : new BigDecimal(lon), order,
        arrival, departure, new BigDecimal(budget), null, 1, null);
  }
}
