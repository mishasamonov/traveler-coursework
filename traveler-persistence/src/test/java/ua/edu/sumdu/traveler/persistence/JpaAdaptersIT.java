package ua.edu.sumdu.traveler.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.dao.PersistenceExceptionTranslationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import ua.edu.sumdu.traveler.core.domain.Location;
import ua.edu.sumdu.traveler.core.domain.PageResult;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria.Scope;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria.SortOrder;
import ua.edu.sumdu.traveler.core.domain.PlanStatus;
import ua.edu.sumdu.traveler.core.domain.PlanSummary;
import ua.edu.sumdu.traveler.core.domain.TravelPlan;
import ua.edu.sumdu.traveler.core.domain.TravelStatistics;
import ua.edu.sumdu.traveler.core.error.ConflictException;
import ua.edu.sumdu.traveler.core.port.LocationPort;
import ua.edu.sumdu.traveler.core.port.TravelPlanPort;

/**
 * Adapters against real PostgreSQL 16 with the TravelerAPI schema and the demo data. Every test
 * runs in a transaction that is rolled back.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// As in the application: @Repository adapters translate SQL errors to DataAccessException.
@ImportAutoConfiguration(PersistenceExceptionTranslationAutoConfiguration.class)
class JpaAdaptersIT {

  static final UUID OLENA = UUID.fromString("c72f743e-3a99-54df-a3db-b799b8a9393e");
  static final LocalDate TODAY = LocalDate.of(2026, 11, 25);

  @Autowired TravelPlanPort plans;
  @Autowired LocationPort locations;
  @Autowired EntityManager em;

  @Test
  @DisplayName("Новий план має версію 1, кожне оновлення збільшує її, застаріла версія — конфлікт")
  void optimisticVersion() {
    TravelPlan plan = plans.insert(plan("Тест версій", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 2)));
    assertThat(plan.version()).isEqualTo(1);
    TravelPlan updated = plans.update(rename(plan, "Нова назва", 1));
    assertThat(updated.version()).isEqualTo(2);
    assertThatThrownBy(() -> plans.update(rename(plan, "Стара вкладка", 1)))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("Перестановка локацій: відкладена перевірка UNIQUE(travel_plan_id, visit_order)")
  void reorderWithDeferredConstraint() {
    TravelPlan plan = plans.insert(plan("Перестановка", null, null));
    Location a = locations.insert(location(plan.id(), "A", 1));
    Location b = locations.insert(location(plan.id(), "B", 2));
    Location c = locations.insert(location(plan.id(), "C", 3));
    locations.reorder(plan.id(), List.of(c.id(), b.id(), a.id()));
    // Forces PostgreSQL to check the deferred constraint now instead of at commit.
    em.createNativeQuery("SET CONSTRAINTS ALL IMMEDIATE").executeUpdate();
    List<Location> route = locations.findByPlanId(plan.id());
    assertThat(route).extracting(Location::name).containsExactly("C", "B", "A");
    assertThat(route).extracting(Location::visitOrder).containsExactly(1, 2, 3);
    assertThat(route.get(0).version()).isEqualTo(2); // moved
    assertThat(route.get(1).version()).isEqualTo(1); // stayed in place
  }

  @Test
  @DisplayName("База даних сама не дозволяє дві локації з однаковою позицією")
  void duplicatePositionRejectedByDatabase() {
    TravelPlan plan = plans.insert(plan("Дублікат", null, null));
    locations.insert(location(plan.id(), "A", 1));
    assertThatThrownBy(() -> locations.insert(location(plan.id(), "B", 1)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("CHECK plan_dates: дата завершення не може передувати даті початку")
  void datesCheckConstraint() {
    assertThatThrownBy(() -> plans.insert(plan("Дати", LocalDate.of(2027, 5, 5), LocalDate.of(2027, 5, 1))))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("Видалення плану видаляє його локації (ON DELETE CASCADE)")
  void cascadeDelete() {
    TravelPlan plan = plans.insert(plan("Каскад", null, null));
    locations.insert(location(plan.id(), "A", 1));
    plans.deleteById(plan.id());
    em.clear();
    assertThat(locations.findByPlanId(plan.id())).isEmpty();
  }

  @Test
  @DisplayName("Пошук: місце кирилицею, статус, «мої плани», сортування й агрегати маршруту")
  void search() {
    assertThat(titles(criteria(Scope.PUBLIC, null).place("львів").build()))
        .containsExactly("Львів на вихідні");
    assertThat(titles(criteria(Scope.PUBLIC, null).status(PlanStatus.COMPLETED).build()))
        .containsExactly("Полтава — Диканька");
    assertThat(titles(criteria(Scope.PUBLIC, null).status(PlanStatus.IN_PROGRESS).build()))
        .containsExactly("Буковель: відкриття зимового сезону");
    assertThat(titles(criteria(Scope.OWNED, OLENA).build()))
        .hasSize(4).contains("Італія: Рим — Флоренція");
    assertThat(titles(criteria(Scope.PUBLIC, null).build())).doesNotContain("Італія: Рим — Флоренція");
    assertThat(titles(criteria(Scope.ALL, null).owner("андрій").build())).hasSize(4);
    assertThat(titles(criteria(Scope.PUBLIC, null).period(LocalDate.of(2027, 5, 3), LocalDate.of(2027, 5, 3)).build()))
        .containsExactly("Карпати: Яремче — Ворохта — Говерла");

    PageResult<PlanSummary> uah = plans.search(criteria(Scope.PUBLIC, null).currency("UAH")
        .sort(SortOrder.BUDGET_DESC).build());
    assertThat(uah.content().get(0).title()).isEqualTo("Буковель: відкриття зимового сезону");
    PlanSummary carpathians = uah.content().stream()
        .filter(p -> p.title().startsWith("Карпати")).findFirst().orElseThrow();
    assertThat(carpathians.locationCount()).isEqualTo(4);
    assertThat(carpathians.plannedSpend()).isEqualByComparingTo("11000.00");
    assertThat(carpathians.ownerName()).isEqualTo("Олена Коваль");
  }

  @Test
  @DisplayName("Статистика для адміністратора і Actuator")
  void statistics() {
    TravelStatistics s = plans.statistics();
    assertThat(s.plans()).isEqualTo(8);
    assertThat(s.publicPlans()).isEqualTo(6);
    assertThat(s.locations()).isEqualTo(28);
    assertThat(s.users()).isEqualTo(3);
    assertThat(s.administrators()).isEqualTo(1);
    assertThat(s.plansWithOrderGaps()).isZero();
    assertThat(s.averageBudgetByCurrency()).containsKeys("UAH", "EUR", "PLN");
  }

  // ---------------------------------------------------------------- helpers

  private List<String> titles(PlanSearchCriteria c) {
    return plans.search(c).content().stream().map(PlanSummary::title).toList();
  }

  private static TravelPlan plan(String title, LocalDate start, LocalDate end) {
    return new TravelPlan(null, OLENA, title, null, start, end, new BigDecimal("100.00"), "UAH",
        false, 1, null, null);
  }

  private static TravelPlan rename(TravelPlan p, String title, int version) {
    return new TravelPlan(p.id(), p.ownerId(), title, p.description(), p.startDate(), p.endDate(),
        p.budget(), p.currency(), p.isPublic(), version, p.createdAt(), p.updatedAt());
  }

  private static Location location(UUID planId, String name, int order) {
    return new Location(null, planId, name, null, null, null, order, null, null, null, null, 1, null);
  }

  private static CriteriaBuilder criteria(Scope scope, UUID ownerId) {
    return new CriteriaBuilder(scope, ownerId);
  }

  /** Small builder that keeps the tests readable. */
  private static final class CriteriaBuilder {
    private final Scope scope;
    private final UUID ownerId;
    private String place;
    private String owner;
    private String currency;
    private PlanStatus status;
    private LocalDate from;
    private LocalDate to;
    private SortOrder sort = SortOrder.NEWEST;

    CriteriaBuilder(Scope scope, UUID ownerId) { this.scope = scope; this.ownerId = ownerId; }
    CriteriaBuilder place(String v) { place = v; return this; }
    CriteriaBuilder owner(String v) { owner = v; return this; }
    CriteriaBuilder currency(String v) { currency = v; return this; }
    CriteriaBuilder status(PlanStatus v) { status = v; return this; }
    CriteriaBuilder period(LocalDate a, LocalDate b) { from = a; to = b; return this; }
    CriteriaBuilder sort(SortOrder v) { sort = v; return this; }

    PlanSearchCriteria build() {
      return new PlanSearchCriteria(scope, ownerId, null, place, owner, currency, null, null, from,
          to, status, sort, 0, 50, TODAY);
    }
  }
}
