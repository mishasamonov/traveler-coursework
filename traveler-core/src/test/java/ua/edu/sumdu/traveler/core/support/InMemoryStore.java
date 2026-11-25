package ua.edu.sumdu.traveler.core.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import ua.edu.sumdu.traveler.core.domain.AppUser;
import ua.edu.sumdu.traveler.core.domain.Location;
import ua.edu.sumdu.traveler.core.domain.PageResult;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria;
import ua.edu.sumdu.traveler.core.domain.PlanSummary;
import ua.edu.sumdu.traveler.core.domain.Role;
import ua.edu.sumdu.traveler.core.domain.TravelPlan;
import ua.edu.sumdu.traveler.core.domain.TravelStatistics;
import ua.edu.sumdu.traveler.core.error.ConflictException;
import ua.edu.sumdu.traveler.core.port.LocationPort;
import ua.edu.sumdu.traveler.core.port.TravelPlanPort;
import ua.edu.sumdu.traveler.core.port.UserPort;

/** In-memory implementation of the ports: the business rules are tested without a database. */
public class InMemoryStore {

  public final Map<UUID, TravelPlan> plans = new LinkedHashMap<>();
  public final Map<UUID, Location> locations = new LinkedHashMap<>();
  public final Map<UUID, AppUser> users = new LinkedHashMap<>();
  /** Plans whose rows were locked (SELECT ... FOR UPDATE) by the services. */
  public final List<UUID> locked = new ArrayList<>();

  public final TravelPlanPort planPort = new Plans();
  public final LocationPort locationPort = new Locations();
  public final UserPort userPort = new Users();

  public AppUser addUser(String email, Role role) {
    return userPort.insert(new AppUser(null, email, email, "hash", role, true, null));
  }

  public List<Location> route(UUID planId) {
    return locationPort.findByPlanId(planId);
  }

  private class Plans implements TravelPlanPort {
    @Override public Optional<TravelPlan> findById(UUID id) { return Optional.ofNullable(plans.get(id)); }

    @Override public Optional<TravelPlan> lockById(UUID id) {
      locked.add(id);
      return findById(id);
    }

    @Override public TravelPlan insert(TravelPlan p) {
      UUID id = UUID.randomUUID();
      TravelPlan saved = new TravelPlan(id, p.ownerId(), p.title(), p.description(), p.startDate(),
          p.endDate(), p.budget(), p.currency(), p.isPublic(), 1, Instant.now(), Instant.now());
      plans.put(id, saved);
      return saved;
    }

    @Override public TravelPlan update(TravelPlan p) {
      TravelPlan current = plans.get(p.id());
      if (current.version() != p.version()) throw new ConflictException("stale", current.version());
      TravelPlan saved = new TravelPlan(p.id(), current.ownerId(), p.title(), p.description(),
          p.startDate(), p.endDate(), p.budget(), p.currency(), p.isPublic(), p.version() + 1,
          current.createdAt(), Instant.now());
      plans.put(p.id(), saved);
      return saved;
    }

    @Override public void deleteById(UUID id) {
      plans.remove(id);
      locations.values().removeIf(l -> l.travelPlanId().equals(id)); // ON DELETE CASCADE
    }

    @Override public PageResult<PlanSummary> search(PlanSearchCriteria c) {
      List<PlanSummary> rows = plans.values().stream()
          .filter(p -> switch (c.scope()) {
            case PUBLIC -> p.isPublic();
            case OWNED -> c.ownerId().equals(p.ownerId());
            case ALL -> true;
          })
          .map(p -> new PlanSummary(p.id(), p.title(), p.description(), p.startDate(), p.endDate(),
              p.budget(), p.currency(), p.isPublic(), p.ownerId(), null,
              route(p.id()).size(), null, p.updatedAt(), p.status(c.today())))
          .toList();
      return new PageResult<>(rows, c.page(), c.size(), rows.size());
    }

    @Override public TravelStatistics statistics() {
      return new TravelStatistics(users.size(), 0, 0, plans.size(), 0, 0, locations.size(), 0,
          Map.of(), List.of(), 0);
    }
  }

  private class Locations implements LocationPort {
    @Override public List<Location> findByPlanId(UUID planId) {
      return locations.values().stream().filter(l -> l.travelPlanId().equals(planId))
          .sorted(Comparator.comparingInt(Location::visitOrder)).toList();
    }

    @Override public Optional<Location> findById(UUID id) { return Optional.ofNullable(locations.get(id)); }

    @Override public int maxVisitOrder(UUID planId) {
      return findByPlanId(planId).stream().mapToInt(Location::visitOrder).max().orElse(0);
    }

    @Override public Location insert(Location l) {
      UUID id = UUID.randomUUID();
      Location saved = new Location(id, l.travelPlanId(), l.name(), l.address(), l.latitude(),
          l.longitude(), l.visitOrder(), l.arrivalDate(), l.departureDate(), l.budget(), l.notes(),
          1, Instant.now());
      locations.put(id, saved);
      return saved;
    }

    @Override public Location update(Location l) {
      Location current = locations.get(l.id());
      if (current.version() != l.version()) throw new ConflictException("stale", current.version());
      Location saved = new Location(l.id(), current.travelPlanId(), l.name(), l.address(),
          l.latitude(), l.longitude(), current.visitOrder(), l.arrivalDate(), l.departureDate(),
          l.budget(), l.notes(), current.version() + 1, current.createdAt());
      locations.put(l.id(), saved);
      return saved;
    }

    @Override public void deleteById(UUID id) { locations.remove(id); }

    @Override public void reorder(UUID planId, List<UUID> orderedIds) {
      for (int i = 0; i < orderedIds.size(); i++) {
        Location l = locations.get(orderedIds.get(i));
        if (l.visitOrder() != i + 1) {
          locations.put(l.id(), new Location(l.id(), l.travelPlanId(), l.name(), l.address(),
              l.latitude(), l.longitude(), i + 1, l.arrivalDate(), l.departureDate(), l.budget(),
              l.notes(), l.version() + 1, l.createdAt()));
        }
      }
    }
  }

  private class Users implements UserPort {
    @Override public Optional<AppUser> findById(UUID id) { return Optional.ofNullable(users.get(id)); }

    @Override public Optional<AppUser> findByEmail(String email) {
      return users.values().stream().filter(u -> u.email().equals(email)).findFirst();
    }

    @Override public boolean existsByEmail(String email) { return findByEmail(email).isPresent(); }

    @Override public AppUser insert(AppUser u) {
      UUID id = UUID.randomUUID();
      AppUser saved = new AppUser(id, u.email(), u.displayName(), u.passwordHash(), u.role(),
          u.enabled(), Instant.now());
      users.put(id, saved);
      return saved;
    }

    @Override public AppUser update(AppUser u) {
      users.put(u.id(), u);
      return u;
    }

    @Override public List<AppUser> findAll() { return List.copyOf(users.values()); }

    @Override public long countEnabledByRole(Role role) {
      return users.values().stream().filter(u -> u.role() == role && u.enabled()).count();
    }
  }
}
