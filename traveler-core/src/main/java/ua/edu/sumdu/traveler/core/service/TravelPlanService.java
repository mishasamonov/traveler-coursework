package ua.edu.sumdu.traveler.core.service;

import static ua.edu.sumdu.traveler.core.service.InputRules.clean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.sumdu.traveler.core.command.PlanCommand;
import ua.edu.sumdu.traveler.core.domain.Actor;
import ua.edu.sumdu.traveler.core.domain.AppUser;
import ua.edu.sumdu.traveler.core.domain.Location;
import ua.edu.sumdu.traveler.core.domain.PageResult;
import ua.edu.sumdu.traveler.core.domain.PlanDetails;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria.Scope;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria.SortOrder;
import ua.edu.sumdu.traveler.core.domain.PlanSummary;
import ua.edu.sumdu.traveler.core.domain.TravelPlan;
import ua.edu.sumdu.traveler.core.domain.TravelStatistics;
import ua.edu.sumdu.traveler.core.error.ConflictException;
import ua.edu.sumdu.traveler.core.error.ForbiddenOperationException;
import ua.edu.sumdu.traveler.core.error.NotFoundException;
import ua.edu.sumdu.traveler.core.error.ValidationException;
import ua.edu.sumdu.traveler.core.port.LocationPort;
import ua.edu.sumdu.traveler.core.port.TravelPlanPort;
import ua.edu.sumdu.traveler.core.port.UserPort;

/**
 * Use cases of travel plans: catalogue search, plan page, creating, editing, publishing, copying
 * and deleting. Every method receives the actor and checks the actor's rights itself, so the
 * rules hold no matter which controller calls the service.
 */
@Transactional
public class TravelPlanService {

  private static final Logger log = LoggerFactory.getLogger(TravelPlanService.class);
  private static final Pattern CURRENCY = Pattern.compile("^[A-Z]{3}$");
  /** Currencies offered in the form; the database accepts any three-letter ISO 4217 code. */
  public static final List<String> COMMON_CURRENCIES =
      List.of("UAH", "EUR", "USD", "PLN", "GBP", "CZK", "HUF", "CHF", "TRY", "RON");
  static final int MAX_PAGE_SIZE = 50;
  static final int MAX_FILTER_LENGTH = 100;

  private final TravelPlanPort plans;
  private final LocationPort locations;
  private final UserPort users;
  private final RouteCalculator calculator;
  private final Clock clock;

  public TravelPlanService(TravelPlanPort plans, LocationPort locations, UserPort users,
      RouteCalculator calculator, Clock clock) {
    this.plans = plans;
    this.locations = locations;
    this.users = users;
    this.calculator = calculator;
    this.clock = clock;
  }

  // ---------------------------------------------------------------- queries

  /**
   * Plan lists: the public catalogue for everybody, "my plans" for a registered user and all
   * plans for the administrator.
   */
  @Transactional(readOnly = true)
  public PageResult<PlanSummary> search(Actor actor, PlanSearchCriteria criteria) {
    Scope scope = criteria.scope() == null ? Scope.PUBLIC : criteria.scope();
    UUID ownerId = null;
    if (scope == Scope.OWNED) {
      requireAuthenticated(actor);
      ownerId = actor.userId();
    } else if (scope == Scope.ALL) {
      requireAdmin(actor);
    }
    return plans.search(validated(criteria).with(scope, ownerId, today()));
  }

  /** Plan page. A private plan of somebody else looks exactly like a missing one. */
  @Transactional(readOnly = true)
  public PlanDetails details(Actor actor, UUID planId) {
    TravelPlan plan = visible(actor, planId);
    List<Location> route = locations.findByPlanId(planId);
    String ownerName = plan.ownerId() == null ? null
        : users.findById(plan.ownerId()).map(AppUser::displayName).orElse(null);
    return new PlanDetails(plan, ownerName, route, calculator.summarize(plan, route),
        plan.status(today()), actor.canEdit(plan), actor.isAuthenticated());
  }

  /** The plan for the edit form; only the owner and the administrator get it. */
  @Transactional(readOnly = true)
  public TravelPlan forEdit(Actor actor, UUID planId) {
    return editable(actor, plans.findById(planId), planId);
  }

  @Transactional(readOnly = true)
  public TravelStatistics statistics(Actor actor) {
    requireAdmin(actor);
    return plans.statistics();
  }

  /**
   * The same figures for system monitoring (the "travel" Actuator endpoint, health indicator).
   * Access to Actuator is restricted to the administrator by Spring Security.
   */
  @Transactional(readOnly = true)
  public TravelStatistics monitoringSnapshot() {
    return plans.statistics();
  }

  // ---------------------------------------------------------------- commands

  public TravelPlan create(Actor actor, PlanCommand command) {
    requireAuthenticated(actor);
    TravelPlan data = validated(null, actor.userId(), command, 1);
    TravelPlan saved = plans.insert(data);
    log.info("Plan created: id={}, owner={}, title='{}', public={}",
        saved.id(), actor.email(), saved.title(), saved.isPublic());
    return saved;
  }

  public TravelPlan update(Actor actor, UUID planId, PlanCommand command) {
    TravelPlan current = editable(actor, plans.findById(planId), planId);
    checkVersion(command.version(), current.version());
    TravelPlan saved = plans.update(validated(current, current.ownerId(), command,
        command.version()));
    log.info("Plan updated: id={}, by={}, version {} -> {}",
        planId, actor.email(), command.version(), saved.version());
    return saved;
  }

  /** Publishes the plan in the catalogue or hides it; uses the same optimistic check. */
  public TravelPlan changeVisibility(Actor actor, UUID planId, boolean makePublic, int version) {
    TravelPlan current = editable(actor, plans.findById(planId), planId);
    checkVersion(version, current.version());
    TravelPlan saved = plans.update(new TravelPlan(current.id(), current.ownerId(),
        current.title(), current.description(), current.startDate(), current.endDate(),
        current.budget(), current.currency(), makePublic, version, current.createdAt(),
        current.updatedAt()));
    log.info("Plan {}: id={}, by={}", makePublic ? "published" : "hidden", planId, actor.email());
    return saved;
  }

  public void delete(Actor actor, UUID planId) {
    TravelPlan plan = editable(actor, plans.findById(planId), planId);
    plans.deleteById(planId);
    log.info("Plan deleted: id={}, title='{}', by={}", planId, plan.title(), actor.email());
  }

  /**
   * Copies a visible plan with its whole route into the actor's own private plans, so a route
   * from the catalogue can be used as a template.
   */
  public TravelPlan copy(Actor actor, UUID planId) {
    requireAuthenticated(actor);
    TravelPlan source = visible(actor, planId);
    TravelPlan copy = plans.insert(new TravelPlan(null, actor.userId(),
        InputRules.truncate(source.title() + " (копія)", 200), source.description(),
        source.startDate(), source.endDate(), source.budget(), source.currency(), false, 1,
        null, null));
    List<Location> route = locations.findByPlanId(planId);
    for (Location l : route) {
      locations.insert(new Location(null, copy.id(), l.name(), l.address(), l.latitude(),
          l.longitude(), l.visitOrder(), l.arrivalDate(), l.departureDate(), l.budget(),
          l.notes(), 1, null));
    }
    log.info("Plan copied: source={}, copy={}, locations={}, by={}",
        planId, copy.id(), route.size(), actor.email());
    return copy;
  }

  // ---------------------------------------------------------------- rules

  private TravelPlan visible(Actor actor, UUID planId) {
    return plans.findById(planId).filter(actor::canView)
        .orElseThrow(() -> new NotFoundException("План подорожі не знайдено"));
  }

  static TravelPlan editable(Actor actor, java.util.Optional<TravelPlan> found, UUID planId) {
    TravelPlan plan = found.filter(actor::canView)
        .orElseThrow(() -> new NotFoundException("План подорожі не знайдено"));
    if (!actor.canEdit(plan)) {
      throw new ForbiddenOperationException(
          "Змінювати план може лише його автор або адміністратор");
    }
    return plan;
  }

  static void checkVersion(Integer expected, int actual) {
    if (expected == null) throw new ValidationException("Не передано версію запису");
    if (expected != actual) {
      throw new ConflictException("Запис уже змінено в іншому вікні або іншим користувачем "
          + "(ваша версія " + expected + ", поточна " + actual + "). Оновіть сторінку й повторіть.",
          actual);
    }
  }

  static void requireAuthenticated(Actor actor) {
    if (!actor.isAuthenticated()) {
      throw new ForbiddenOperationException("Операція доступна лише зареєстрованим користувачам");
    }
  }

  static void requireAdmin(Actor actor) {
    if (!actor.isAdmin()) {
      throw new ForbiddenOperationException("Операція доступна лише адміністратору");
    }
  }

  private TravelPlan validated(TravelPlan current, UUID ownerId, PlanCommand c, int version) {
    String title = InputRules.required(c.title(), "title", "Назва подорожі", 200);
    String description = InputRules.optional(c.description(), "description", "Опис", 5000);
    if (c.startDate() != null && c.endDate() != null && c.endDate().isBefore(c.startDate())) {
      throw new ValidationException("endDate",
          "Дата завершення не може передувати даті початку подорожі");
    }
    if (c.startDate() == null && c.endDate() != null) {
      throw new ValidationException("startDate", "Вкажіть дату початку подорожі");
    }
    BigDecimal budget = InputRules.money(c.budget(), "budget", "Бюджет");
    String currency = clean(c.currency());
    if (currency == null || !CURRENCY.matcher(currency).matches()) {
      throw new ValidationException("currency", "Валюта — три великі латинські літери (ISO 4217)");
    }
    return new TravelPlan(current == null ? null : current.id(), ownerId, title, description,
        c.startDate(), c.endDate(), budget, currency, c.isPublic(), version,
        current == null ? null : current.createdAt(), current == null ? null : current.updatedAt());
  }

  private PlanSearchCriteria validated(PlanSearchCriteria c) {
    if (c.page() < 0) throw new ValidationException("Номер сторінки не може бути від'ємним");
    if (c.size() < 1 || c.size() > MAX_PAGE_SIZE) {
      throw new ValidationException("Розмір сторінки має бути від 1 до " + MAX_PAGE_SIZE);
    }
    String text = InputRules.optional(c.text(), "text", "Пошуковий запит", MAX_FILTER_LENGTH);
    String place = InputRules.optional(c.place(), "place", "Локація", MAX_FILTER_LENGTH);
    String owner = InputRules.optional(c.owner(), "owner", "Власник", MAX_FILTER_LENGTH);
    String currency = clean(c.currency());
    if (currency != null && !CURRENCY.matcher(currency).matches()) {
      throw new ValidationException("currency", "Невідома валюта");
    }
    if (c.minBudget() != null && c.maxBudget() != null && c.minBudget().compareTo(c.maxBudget()) > 0) {
      throw new ValidationException("maxBudget", "Мінімальний бюджет більший за максимальний");
    }
    if (c.dateFrom() != null && c.dateTo() != null && c.dateTo().isBefore(c.dateFrom())) {
      throw new ValidationException("dateTo", "Кінець періоду раніше за його початок");
    }
    SortOrder sort = c.sort() == null ? SortOrder.NEWEST : c.sort();
    return new PlanSearchCriteria(c.scope(), null, text, place, owner, currency, c.minBudget(),
        c.maxBudget(), c.dateFrom(), c.dateTo(), c.status(), sort, c.page(), c.size(), null);
  }

  private LocalDate today() {
    return LocalDate.now(clock);
  }
}
