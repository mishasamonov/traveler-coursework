package ua.edu.sumdu.traveler.web.form;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.util.UriComponentsBuilder;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria.Scope;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria.SortOrder;
import ua.edu.sumdu.traveler.core.domain.PlanStatus;

/** Search and filter parameters of plan lists, bound from the query string (GET form). */
public class PlanFilterForm {

  public static final int PAGE_SIZE = 9;

  private String text;
  private String place;
  private String owner;
  private String currency;
  private BigDecimal minBudget;
  private BigDecimal maxBudget;
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate dateFrom;
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate dateTo;
  private PlanStatus status;
  private SortOrder sort = SortOrder.NEWEST;
  private int page;

  public PlanSearchCriteria toCriteria(Scope scope) {
    return new PlanSearchCriteria(scope, null, text, place, owner, blankToNull(currency),
        minBudget, maxBudget, dateFrom, dateTo, status, sort, Math.max(page, 0), PAGE_SIZE, null);
  }

  /** True when at least one filter is set (the page then shows a "reset" link). */
  public boolean isFiltered() {
    return notBlank(text) || notBlank(place) || notBlank(owner) || notBlank(currency)
        || minBudget != null || maxBudget != null || dateFrom != null || dateTo != null
        || status != null;
  }

  /** Link to another page of the same list with the same filters. */
  public String pageUrl(String path, int targetPage) {
    UriComponentsBuilder b = UriComponentsBuilder.fromPath(path);
    add(b, "text", text);
    add(b, "place", place);
    add(b, "owner", owner);
    add(b, "currency", currency);
    add(b, "minBudget", minBudget);
    add(b, "maxBudget", maxBudget);
    add(b, "dateFrom", dateFrom);
    add(b, "dateTo", dateTo);
    add(b, "status", status == null ? null : status.name());
    add(b, "sort", sort == null || sort == SortOrder.NEWEST ? null : sort.name());
    if (targetPage > 0) b.queryParam("page", targetPage);
    return b.encode().build().toUriString();
  }

  private static void add(UriComponentsBuilder b, String name, Object value) {
    if (value != null && !value.toString().isBlank()) b.queryParam(name, value);
  }

  private static boolean notBlank(String s) {
    return s != null && !s.isBlank();
  }

  private static String blankToNull(String s) {
    return notBlank(s) ? s : null;
  }

  public String getText() { return text; }
  public void setText(String text) { this.text = text; }
  public String getPlace() { return place; }
  public void setPlace(String place) { this.place = place; }
  public String getOwner() { return owner; }
  public void setOwner(String owner) { this.owner = owner; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public BigDecimal getMinBudget() { return minBudget; }
  public void setMinBudget(BigDecimal minBudget) { this.minBudget = minBudget; }
  public BigDecimal getMaxBudget() { return maxBudget; }
  public void setMaxBudget(BigDecimal maxBudget) { this.maxBudget = maxBudget; }
  public LocalDate getDateFrom() { return dateFrom; }
  public void setDateFrom(LocalDate dateFrom) { this.dateFrom = dateFrom; }
  public LocalDate getDateTo() { return dateTo; }
  public void setDateTo(LocalDate dateTo) { this.dateTo = dateTo; }
  public PlanStatus getStatus() { return status; }
  public void setStatus(PlanStatus status) { this.status = status; }
  public SortOrder getSort() { return sort; }
  public void setSort(SortOrder sort) { this.sort = sort; }
  public int getPage() { return page; }
  public void setPage(int page) { this.page = page; }
}
