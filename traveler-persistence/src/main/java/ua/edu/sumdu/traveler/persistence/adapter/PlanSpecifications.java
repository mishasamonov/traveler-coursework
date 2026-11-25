package ua.edu.sumdu.traveler.persistence.adapter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria;
import ua.edu.sumdu.traveler.persistence.entity.LocationEntity;
import ua.edu.sumdu.traveler.persistence.entity.TravelPlanEntity;
import ua.edu.sumdu.traveler.persistence.entity.UserEntity;

/**
 * Builds the WHERE clause of plan search from the filled-in filters only (JPA Criteria API via
 * Spring Data Specifications), so one query serves the catalogue, "my plans" and the admin list.
 */
final class PlanSpecifications {

  private PlanSpecifications() {}

  static Specification<TravelPlanEntity> matching(PlanSearchCriteria c) {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      switch (c.scope()) {
        case PUBLIC -> where.add(cb.isTrue(root.get("publicPlan")));
        case OWNED -> where.add(cb.equal(root.get("owner").get("id"), c.ownerId()));
        case ALL -> { /* administrator: no restriction */ }
      }
      if (c.text() != null) {
        String like = like(c.text());
        where.add(cb.or(
            cb.like(cb.lower(root.get("title")), like, '\\'),
            cb.like(cb.lower(root.get("description")), like, '\\')));
      }
      if (c.place() != null) {
        // EXISTS (SELECT 1 FROM locations l WHERE l.travel_plan_id = p.id AND (name|address) LIKE)
        String like = like(c.place());
        Subquery<Integer> sub = query.subquery(Integer.class);
        Root<LocationEntity> l = sub.from(LocationEntity.class);
        sub.select(cb.literal(1)).where(
            cb.equal(l.get("travelPlanId"), root.get("id")),
            cb.or(cb.like(cb.lower(l.get("name")), like, '\\'),
                cb.like(cb.lower(l.get("address")), like, '\\')));
        where.add(cb.exists(sub));
      }
      if (c.owner() != null) {
        String like = like(c.owner());
        Subquery<Integer> sub = query.subquery(Integer.class);
        Root<UserEntity> u = sub.from(UserEntity.class);
        sub.select(cb.literal(1)).where(
            cb.equal(u, root.get("owner")),
            cb.or(cb.like(cb.lower(u.get("email")), like, '\\'),
                cb.like(cb.lower(u.get("displayName")), like, '\\')));
        where.add(cb.exists(sub));
      }
      if (c.currency() != null) where.add(cb.equal(root.get("currency"), c.currency()));
      if (c.minBudget() != null) where.add(cb.greaterThanOrEqualTo(root.get("budget"), c.minBudget()));
      if (c.maxBudget() != null) where.add(cb.lessThanOrEqualTo(root.get("budget"), c.maxBudget()));

      Expression<LocalDate> start = root.get("startDate");
      Expression<LocalDate> last = cb.coalesce(root.get("endDate"), start);
      // The trip overlaps the period [dateFrom, dateTo].
      if (c.dateFrom() != null) where.add(cb.greaterThanOrEqualTo(last, c.dateFrom()));
      if (c.dateTo() != null) where.add(cb.lessThanOrEqualTo(start, c.dateTo()));
      if (c.status() != null) where.add(status(c, cb, start, last));
      return cb.and(where.toArray(Predicate[]::new));
    };
  }

  /** The same rules as PlanStatus.of(), expressed in SQL. */
  private static Predicate status(PlanSearchCriteria c, CriteriaBuilder cb,
      Expression<LocalDate> start, Expression<LocalDate> last) {
    LocalDate today = c.today();
    return switch (c.status()) {
      case DRAFT -> cb.isNull(start);
      case UPCOMING -> cb.greaterThan(start, today);
      case IN_PROGRESS -> cb.and(cb.lessThanOrEqualTo(start, today),
          cb.greaterThanOrEqualTo(last, today));
      case COMPLETED -> cb.lessThan(last, today);
    };
  }

  static Sort sort(PlanSearchCriteria.SortOrder order) {
    return switch (order) {
      case NEWEST -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));
      case START_DATE -> Sort.by(Sort.Order.asc("startDate").nullsLast(), Sort.Order.asc("id"));
      case BUDGET_ASC -> Sort.by(Sort.Order.asc("budget").nullsLast(), Sort.Order.asc("id"));
      case BUDGET_DESC -> Sort.by(Sort.Order.desc("budget").nullsLast(), Sort.Order.asc("id"));
      case TITLE -> Sort.by(Sort.Order.asc("title"), Sort.Order.asc("id"));
    };
  }

  /** %text% in lower case with the LIKE wildcards of the user's text escaped. */
  private static String like(String text) {
    String escaped = text.toLowerCase(Locale.ROOT)
        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    return "%" + escaped + "%";
  }
}
