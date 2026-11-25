package ua.edu.sumdu.traveler.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.sumdu.traveler.core.command.LocationCommand;
import ua.edu.sumdu.traveler.core.domain.Actor;
import ua.edu.sumdu.traveler.core.domain.AppUser;
import ua.edu.sumdu.traveler.core.domain.Location;
import ua.edu.sumdu.traveler.core.domain.Role;
import ua.edu.sumdu.traveler.core.domain.TravelPlan;
import ua.edu.sumdu.traveler.core.error.ConflictException;
import ua.edu.sumdu.traveler.core.error.ForbiddenOperationException;
import ua.edu.sumdu.traveler.core.error.ValidationException;
import ua.edu.sumdu.traveler.core.service.LocationService.Direction;
import ua.edu.sumdu.traveler.core.support.InMemoryStore;

class LocationServiceTest {

  private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");
  private InMemoryStore store;
  private LocationService service;
  private Actor owner;
  private Actor stranger;
  private UUID planId;

  @BeforeEach
  void setUp() {
    store = new InMemoryStore();
    service = new LocationService(store.planPort, store.locationPort, KYIV);
    owner = actor(store.addUser("owner@test", Role.USER));
    stranger = actor(store.addUser("stranger@test", Role.USER));
    planId = store.planPort.insert(new TravelPlan(null, owner.userId(), "Маршрут", null,
        LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 5), null, "UAH", true, 1, null, null)).id();
  }

  @Test
  @DisplayName("Позиція нової локації визначається автоматично: 1, 2, 3; план блокується")
  void addAssignsNextPosition() {
    add("A");
    add("B");
    Location c = add("C");
    assertThat(c.visitOrder()).isEqualTo(3);
    assertThat(names()).containsExactly("A", "B", "C");
    assertThat(store.locked).hasSize(3).containsOnly(planId);
  }

  @Test
  @DisplayName("Переміщення вгору/вниз міняє сусідів місцями, крайні позиції — помилка")
  void moveUpAndDown() {
    add("A");
    add("B");
    Location c = add("C");
    service.move(owner, c.id(), Direction.UP, c.version());
    assertThat(names()).containsExactly("A", "C", "B");
    Location a = store.route(planId).get(0);
    assertThatThrownBy(() -> service.move(owner, a.id(), Direction.UP, a.version()))
        .isInstanceOf(ValidationException.class).hasMessageContaining("перша");
    assertThat(orders()).containsExactly(1, 2, 3);
  }

  @Test
  @DisplayName("Переміщення із застарілою версією локації відхиляється")
  void moveWithStaleVersion() {
    Location a = add("A");
    add("B");
    service.move(owner, a.id(), Direction.DOWN, 1); // version of A becomes 2
    assertThatThrownBy(() -> service.move(owner, a.id(), Direction.UP, 1))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("Після видалення нумерація маршруту стає суцільною")
  void deleteRenumbers() {
    add("A");
    Location b = add("B");
    add("C");
    add("D");
    service.delete(owner, b.id());
    assertThat(names()).containsExactly("A", "C", "D");
    assertThat(orders()).containsExactly(1, 2, 3);
  }

  @Test
  @DisplayName("Зміна позиції у формі редагування зсуває інші локації")
  void updateMovesToPosition() {
    Location a = add("A");
    add("B");
    add("C");
    service.update(owner, a.id(), command("A*", 3, a.version()));
    assertThat(names()).containsExactly("B", "C", "A*");
    assertThat(orders()).containsExactly(1, 2, 3);
    Location moved = store.route(planId).get(2);
    assertThatThrownBy(() -> service.update(owner, moved.id(), command("A", 9, moved.version())))
        .isInstanceOf(ValidationException.class).hasMessageContaining("від 1 до 3");
  }

  @Test
  @DisplayName("Час відвідування має бути в межах дат подорожі, координати — парою")
  void validation() {
    OffsetDateTime before = LocalDateTime.of(2027, 4, 30, 12, 0).atZone(KYIV).toOffsetDateTime();
    LocationCommand early = new LocationCommand("A", null, null, null, before, null, null, null,
        null, null);
    assertThatThrownBy(() -> service.add(owner, planId, early))
        .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("arrivalDate");
    LocationCommand half = new LocationCommand("A", null, new BigDecimal("49.84"), null, null,
        null, null, null, null, null);
    assertThatThrownBy(() -> service.add(owner, planId, half)).hasMessageContaining("обидві");
    LocationCommand far = new LocationCommand("A", null, new BigDecimal("91"), BigDecimal.ONE,
        null, null, null, null, null, null);
    assertThatThrownBy(() -> service.add(owner, planId, far)).extracting("field").isEqualTo("latitude");
  }

  @Test
  @DisplayName("Чужий маршрут змінювати не можна")
  void strangerCannotEdit() {
    add("A");
    assertThatThrownBy(() -> service.add(stranger, planId, TravelPlanServiceTest.point("X")))
        .isInstanceOf(ForbiddenOperationException.class);
  }

  private Location add(String name) {
    return service.add(owner, planId, TravelPlanServiceTest.point(name));
  }

  private static LocationCommand command(String name, Integer order, int version) {
    return new LocationCommand(name, null, null, null, null, null, null, null, order, version);
  }

  private java.util.List<String> names() {
    return store.route(planId).stream().map(Location::name).toList();
  }

  private java.util.List<Integer> orders() {
    return store.route(planId).stream().map(Location::visitOrder).toList();
  }

  private static Actor actor(AppUser user) {
    return new Actor(user.id(), user.email(), user.role());
  }
}
