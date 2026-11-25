package ua.edu.sumdu.traveler.core.domain;

import java.time.LocalDate;

/** Stage of a trip, derived from its dates and today's date (it is not stored in the database). */
public enum PlanStatus {
  DRAFT("Без дат"),
  UPCOMING("Заплановано"),
  IN_PROGRESS("Триває"),
  COMPLETED("Завершено");

  private final String label;

  PlanStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public static PlanStatus of(LocalDate start, LocalDate end, LocalDate today) {
    if (start == null) return DRAFT;
    if (today.isBefore(start)) return UPCOMING;
    LocalDate last = end != null ? end : start;
    return today.isAfter(last) ? COMPLETED : IN_PROGRESS;
  }
}
