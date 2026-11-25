package ua.edu.sumdu.traveler.core.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.sumdu.traveler.core.command.LocationCommand;
import ua.edu.sumdu.traveler.core.domain.Actor;
import ua.edu.sumdu.traveler.core.domain.Location;
import ua.edu.sumdu.traveler.core.domain.TravelPlan;
import ua.edu.sumdu.traveler.core.error.NotFoundException;
import ua.edu.sumdu.traveler.core.error.ValidationException;
import ua.edu.sumdu.traveler.core.port.LocationPort;
import ua.edu.sumdu.traveler.core.port.TravelPlanPort;

/**
 * Route management. The position of a new location is assigned automatically (last + 1), the
 * route can be reordered, and after a deletion the positions are renumbered without gaps. Every
 * change of a route first locks the row of its plan, so concurrent requests are serialized and
 * two locations of one plan never get the same visit_order.
 */
@Transactional
public class LocationService {

  private static final Logger log = LoggerFactory.getLogger(LocationService.class);
  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  /** Direction of the "move" buttons of the route table. */
  public enum Direction { UP, DOWN }

  /** A location together with its plan and the route length, for the edit form. */
  public record LocationEdit(TravelPlan plan, Location location, int routeSize) {}

  private final TravelPlanPort plans;
  private final LocationPort locations;
  private final ZoneId zone;

  public LocationService(TravelPlanPort plans, LocationPort locations, ZoneId zone) {
    this.plans = plans;
    this.locations = locations;
    this.zone = zone;
  }

  /** Plan for the "new location" form: only its owner and the administrator may add points. */
  @Transactional(readOnly = true)
  public TravelPlan planForNewLocation(Actor actor, UUID planId) {
    return TravelPlanService.editable(actor, plans.findById(planId), planId);
  }

  @Transactional(readOnly = true)
  public LocationEdit forEdit(Actor actor, UUID locationId) {
    Location location = find(locationId);
    TravelPlan plan = TravelPlanService.editable(actor, plans.findById(location.travelPlanId()),
        location.travelPlanId());
    return new LocationEdit(plan, location, locations.maxVisitOrder(plan.id()));
  }

  /** Adds a location to the end of the route: visit_order = MAX(visit_order) + 1. */
  public Location add(Actor actor, UUID planId, LocationCommand command) {
    TravelPlan plan = lockEditable(actor, planId);
    Location data = validated(plan, null, command);
    int position = locations.maxVisitOrder(planId) + 1;
    Location saved = locations.insert(new Location(null, planId, data.name(), data.address(),
        data.latitude(), data.longitude(), position, data.arrivalDate(), data.departureDate(),
        data.budget(), data.notes(), 1, null));
    log.info("Location added: plan={}, location={}, name='{}', visit_order={}, by={}",
        planId, saved.id(), saved.name(), position, actor.email());
    return saved;
  }

  /**
   * Saves the fields of a location. When the user also changed its position, the other locations
   * move up or down to make room, as in the REST API of the laboratory works.
   */
  public Location update(Actor actor, UUID locationId, LocationCommand command) {
    UUID planId = find(locationId).travelPlanId();
    TravelPlan plan = lockEditable(actor, planId);
    Location current = find(locationId); // read again under the lock of the plan
    TravelPlanService.checkVersion(command.version(), current.version());
    Location data = validated(plan, current, command);
    locations.update(new Location(current.id(), planId, data.name(), data.address(),
        data.latitude(), data.longitude(), current.visitOrder(), data.arrivalDate(),
        data.departureDate(), data.budget(), data.notes(), command.version(),
        current.createdAt()));
    Integer target = command.visitOrder();
    if (target != null && target != current.visitOrder()) {
      List<UUID> route = ids(locations.findByPlanId(planId));
      if (target < 1 || target > route.size()) {
        throw new ValidationException("visitOrder",
            "Позиція в маршруті має бути від 1 до " + route.size());
      }
      route.remove(locationId);
      route.add(target - 1, locationId);
      locations.reorder(planId, route);
      log.info("Location moved: plan={}, location={}, visit_order {} -> {}",
          planId, locationId, current.visitOrder(), target);
    }
    log.info("Location updated: plan={}, location={}, by={}", planId, locationId, actor.email());
    return find(locationId);
  }

  /** Moves the location one step up or down the route (swaps it with the neighbour). */
  public void move(Actor actor, UUID locationId, Direction direction, int version) {
    UUID planId = find(locationId).travelPlanId();
    lockEditable(actor, planId);
    Location current = find(locationId);
    TravelPlanService.checkVersion(version, current.version());
    List<UUID> route = ids(locations.findByPlanId(planId));
    int index = route.indexOf(locationId);
    int target = direction == Direction.UP ? index - 1 : index + 1;
    if (target < 0 || target >= route.size()) {
      throw new ValidationException(direction == Direction.UP
          ? "Локація вже перша в маршруті" : "Локація вже остання в маршруті");
    }
    route.set(index, route.get(target));
    route.set(target, locationId);
    locations.reorder(planId, route);
    log.info("Location moved {}: plan={}, location={}, visit_order {} -> {}, by={}",
        direction, planId, locationId, index + 1, target + 1, actor.email());
  }

  /** Deletes the location and closes the gap: the following locations move one position up. */
  public void delete(Actor actor, UUID locationId) {
    Location location = find(locationId);
    UUID planId = location.travelPlanId();
    lockEditable(actor, planId);
    locations.deleteById(locationId);
    List<UUID> rest = ids(locations.findByPlanId(planId));
    locations.reorder(planId, rest);
    log.info("Location deleted: plan={}, location={}, name='{}', remaining={}, by={}",
        planId, locationId, location.name(), rest.size(), actor.email());
  }

  // ---------------------------------------------------------------- rules

  private TravelPlan lockEditable(Actor actor, UUID planId) {
    return TravelPlanService.editable(actor, plans.lockById(planId), planId);
  }

  private Location find(UUID locationId) {
    return locations.findById(locationId)
        .orElseThrow(() -> new NotFoundException("Локацію не знайдено"));
  }

  private static List<UUID> ids(List<Location> route) {
    List<UUID> result = new ArrayList<>(route.size());
    route.forEach(l -> result.add(l.id()));
    return result;
  }

  private Location validated(TravelPlan plan, Location current, LocationCommand c) {
    String name = InputRules.required(c.name(), "name", "Назва локації", 200);
    String address = InputRules.optional(c.address(), "address", "Адреса", 1000);
    String notes = InputRules.optional(c.notes(), "notes", "Примітки", 2000);
    BigDecimal latitude = InputRules.coordinate(c.latitude(), "latitude", "Широта", 90);
    BigDecimal longitude = InputRules.coordinate(c.longitude(), "longitude", "Довгота", 180);
    if ((latitude == null) != (longitude == null)) {
      throw new ValidationException(latitude == null ? "latitude" : "longitude",
          "Вкажіть обидві координати або залиште обидві порожніми");
    }
    OffsetDateTime arrival = c.arrivalDate();
    OffsetDateTime departure = c.departureDate();
    if (arrival != null && departure != null && departure.isBefore(arrival)) {
      throw new ValidationException("departureDate", "Час відбуття не може передувати прибуттю");
    }
    checkWithinTrip(plan, arrival, "arrivalDate");
    checkWithinTrip(plan, departure, "departureDate");
    BigDecimal budget = InputRules.money(c.budget(), "budget", "Витрати");
    return new Location(current == null ? null : current.id(), plan.id(), name, address,
        latitude, longitude, current == null ? 0 : current.visitOrder(), arrival, departure,
        budget, notes, current == null ? 1 : current.version(),
        current == null ? null : current.createdAt());
  }

  /** A visit must take place during the trip when the trip has dates. */
  private void checkWithinTrip(TravelPlan plan, OffsetDateTime time, String field) {
    if (time == null) return;
    LocalDate day = time.atZoneSameInstant(zone).toLocalDate();
    if (plan.startDate() != null && day.isBefore(plan.startDate())) {
      throw new ValidationException(field, "Дата раніше за початок подорожі (" + DAY.format(plan.startDate()) + ")");
    }
    if (plan.endDate() != null && day.isAfter(plan.endDate())) {
      throw new ValidationException(field, "Дата пізніше за завершення подорожі (" + DAY.format(plan.endDate()) + ")");
    }
  }
}
