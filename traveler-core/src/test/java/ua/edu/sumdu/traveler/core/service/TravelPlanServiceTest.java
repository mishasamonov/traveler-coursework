package ua.edu.sumdu.traveler.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.sumdu.traveler.core.command.LocationCommand;
import ua.edu.sumdu.traveler.core.command.PlanCommand;
import ua.edu.sumdu.traveler.core.domain.Actor;
import ua.edu.sumdu.traveler.core.domain.AppUser;
import ua.edu.sumdu.traveler.core.domain.PlanDetails;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria.Scope;
import ua.edu.sumdu.traveler.core.domain.Role;
import ua.edu.sumdu.traveler.core.domain.TravelPlan;
import ua.edu.sumdu.traveler.core.error.ConflictException;
import ua.edu.sumdu.traveler.core.error.ForbiddenOperationException;
import ua.edu.sumdu.traveler.core.error.NotFoundException;
import ua.edu.sumdu.traveler.core.error.ValidationException;
import ua.edu.sumdu.traveler.core.support.InMemoryStore;

class TravelPlanServiceTest {

  private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");
  private InMemoryStore store;
  private TravelPlanService service;
  private LocationService locations;
  private Actor olena;
  private Actor andrii;
  private Actor admin;

  @BeforeEach
  void setUp() {
    store = new InMemoryStore();
    Clock clock = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), KYIV);
    service = new TravelPlanService(store.planPort, store.locationPort, store.userPort,
        new RouteCalculator(KYIV), clock);
    locations = new LocationService(store.planPort, store.locationPort, KYIV);
    olena = actor(store.addUser("olena@test", Role.USER));
    andrii = actor(store.addUser("andrii@test", Role.USER));
    admin = actor(store.addUser("admin@test", Role.ADMIN));
  }

  @Test
  @DisplayName("Гість не може створити план; користувач створює план з версією 1")
  void createRequiresAccount() {
    assertThatThrownBy(() -> service.create(Actor.GUEST, command("Львів", false, null)))
        .isInstanceOf(ForbiddenOperationException.class);
    TravelPlan plan = service.create(olena, command("  Львів  ", false, null));
    assertThat(plan.title()).isEqualTo("Львів");
    assertThat(plan.ownerId()).isEqualTo(olena.userId());
    assertThat(plan.version()).isEqualTo(1);
  }

  @Test
  @DisplayName("Бізнес-правила: дата завершення не раніше початку, бюджет не від'ємний, валюта ISO")
  void validation() {
    PlanCommand wrongDates = new PlanCommand("План", null, LocalDate.of(2027, 5, 5),
        LocalDate.of(2027, 5, 1), null, "UAH", false, null);
    assertThatThrownBy(() -> service.create(olena, wrongDates))
        .isInstanceOf(ValidationException.class)
        .extracting("field").isEqualTo("endDate");
    PlanCommand negative = new PlanCommand("План", null, null, null, new BigDecimal("-1"), "UAH",
        false, null);
    assertThatThrownBy(() -> service.create(olena, negative)).hasMessageContaining("від'ємним");
    PlanCommand currency = new PlanCommand("План", null, null, null, null, "hryvnia", false, null);
    assertThatThrownBy(() -> service.create(olena, currency)).extracting("field").isEqualTo("currency");
    PlanCommand blank = new PlanCommand("   ", null, null, null, null, "UAH", false, null);
    assertThatThrownBy(() -> service.create(olena, blank)).extracting("field").isEqualTo("title");
  }

  @Test
  @DisplayName("Приватний план інших не видно (404), публічний не можна змінити (403)")
  void accessRules() {
    TravelPlan secret = service.create(olena, command("Приватний", false, null));
    TravelPlan open = service.create(olena, command("Публічний", true, null));
    assertThatThrownBy(() -> service.details(andrii, secret.id())).isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> service.details(Actor.GUEST, secret.id())).isInstanceOf(NotFoundException.class);
    PlanDetails visible = service.details(andrii, open.id());
    assertThat(visible.editable()).isFalse();
    assertThat(visible.copyable()).isTrue();
    assertThatThrownBy(() -> service.update(andrii, open.id(), command("Чуже", true, 1)))
        .isInstanceOf(ForbiddenOperationException.class);
    assertThatThrownBy(() -> service.delete(andrii, open.id()))
        .isInstanceOf(ForbiddenOperationException.class);
    // The administrator has full control.
    assertThat(service.details(admin, secret.id()).editable()).isTrue();
    assertThat(service.update(admin, secret.id(), command("Змінено адміністратором", false, 1))
        .version()).isEqualTo(2);
  }

  @Test
  @DisplayName("Оптимістичне блокування: застаріла версія форми дає конфлікт")
  void optimisticLocking() {
    TravelPlan plan = service.create(olena, command("Карпати", false, null));
    service.update(olena, plan.id(), command("Карпати (вкладка 1)", false, 1));
    assertThatThrownBy(() -> service.update(olena, plan.id(), command("Карпати (вкладка 2)", false, 1)))
        .isInstanceOf(ConflictException.class)
        .extracting("currentVersion").isEqualTo(2);
    assertThat(store.plans.get(plan.id()).title()).isEqualTo("Карпати (вкладка 1)");
  }

  @Test
  @DisplayName("Копія публічного плану — приватний план нового власника з тим самим маршрутом")
  void copyPublicPlan() {
    TravelPlan source = service.create(olena, command("Одеса", true, null));
    locations.add(olena, source.id(), point("Потьомкінські сходи"));
    locations.add(olena, source.id(), point("Оперний театр"));
    TravelPlan copy = service.copy(andrii, source.id());
    assertThat(copy.ownerId()).isEqualTo(andrii.userId());
    assertThat(copy.isPublic()).isFalse();
    assertThat(copy.title()).isEqualTo("Одеса (копія)");
    assertThat(store.route(copy.id())).extracting("name", "visitOrder")
        .containsExactly(org.assertj.core.groups.Tuple.tuple("Потьомкінські сходи", 1),
            org.assertj.core.groups.Tuple.tuple("Оперний театр", 2));
    assertThatThrownBy(() -> service.copy(Actor.GUEST, source.id()))
        .isInstanceOf(ForbiddenOperationException.class);
  }

  @Test
  @DisplayName("Списки: «мої плани» лише для зареєстрованих, «усі плани» лише для адміністратора")
  void searchScopes() {
    service.create(olena, command("Мій", false, null));
    service.create(andrii, command("Чужий публічний", true, null));
    assertThat(service.search(olena, criteria(Scope.OWNED)).content())
        .extracting("title").containsExactly("Мій");
    assertThat(service.search(Actor.GUEST, criteria(Scope.PUBLIC)).content())
        .extracting("title").containsExactly("Чужий публічний");
    assertThat(service.search(admin, criteria(Scope.ALL)).totalElements()).isEqualTo(2);
    assertThatThrownBy(() -> service.search(Actor.GUEST, criteria(Scope.OWNED)))
        .isInstanceOf(ForbiddenOperationException.class);
    assertThatThrownBy(() -> service.search(olena, criteria(Scope.ALL)))
        .isInstanceOf(ForbiddenOperationException.class);
  }

  @Test
  @DisplayName("Видалення плану прибирає і його локації (ON DELETE CASCADE)")
  void deleteCascades() {
    TravelPlan plan = service.create(olena, command("Тимчасовий", false, null));
    locations.add(olena, plan.id(), point("A"));
    service.delete(olena, plan.id());
    assertThat(store.plans).isEmpty();
    assertThat(store.locations).isEmpty();
  }

  private static PlanSearchCriteria criteria(Scope scope) {
    return new PlanSearchCriteria(scope, null, null, null, null, null, null, null, null, null,
        null, null, 0, 10, null);
  }

  private static PlanCommand command(String title, boolean isPublic, Integer version) {
    return new PlanCommand(title, "опис", null, null, new BigDecimal("1000"), "UAH", isPublic,
        version);
  }

  static LocationCommand point(String name) {
    return new LocationCommand(name, null, null, null, null, null, null, null, null, null);
  }

  private static Actor actor(AppUser user) {
    return new Actor(user.id(), user.email(), user.role());
  }
}
