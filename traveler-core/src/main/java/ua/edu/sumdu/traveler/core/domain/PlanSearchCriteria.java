package ua.edu.sumdu.traveler.core.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Search and filter parameters of plan lists.
 *
 * @param text words in the title or the description
 * @param place name or address of a location the trip visits (for example "Львів")
 * @param owner part of the owner's e-mail or name (administrator only)
 * @param dateFrom with dateTo: trips that overlap this period
 * @param today the reference date for the status filter, set by the service
 */
public record PlanSearchCriteria(
    Scope scope,
    UUID ownerId,
    String text,
    String place,
    String owner,
    String currency,
    BigDecimal minBudget,
    BigDecimal maxBudget,
    LocalDate dateFrom,
    LocalDate dateTo,
    PlanStatus status,
    SortOrder sort,
    int page,
    int size,
    LocalDate today) {

  /** Which plans a list may contain. */
  public enum Scope {
    /** Public catalogue: is_public = true. */
    PUBLIC,
    /** Plans of one owner, public and private. */
    OWNED,
    /** All plans, including private and ownerless ones (administrator). */
    ALL
  }

  public enum SortOrder {
    NEWEST("Спочатку нові"),
    START_DATE("За датою початку"),
    BUDGET_ASC("Бюджет за зростанням"),
    BUDGET_DESC("Бюджет за спаданням"),
    TITLE("За назвою");

    private final String label;

    SortOrder(String label) {
      this.label = label;
    }

    public String label() {
      return label;
    }
  }

  public static PlanSearchCriteria publicCatalog() {
    return new PlanSearchCriteria(Scope.PUBLIC, null, null, null, null, null, null, null, null,
        null, null, SortOrder.NEWEST, 0, 9, null);
  }

  public PlanSearchCriteria with(Scope newScope, UUID newOwnerId, LocalDate newToday) {
    return new PlanSearchCriteria(newScope, newOwnerId, text, place, owner, currency, minBudget,
        maxBudget, dateFrom, dateTo, status, sort, page, size, newToday);
  }
}
