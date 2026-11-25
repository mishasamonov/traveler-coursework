package ua.edu.sumdu.traveler.core.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ua.edu.sumdu.traveler.core.domain.Location;

/** Data access to route locations. Callers hold the lock of the parent plan (TravelPlanPort). */
public interface LocationPort {

  /** Locations of the plan ordered by visit_order. */
  List<Location> findByPlanId(UUID planId);

  Optional<Location> findById(UUID id);

  /** The largest visit_order of the plan or 0 when the route is empty. */
  int maxVisitOrder(UUID planId);

  /** Inserts a location with version 1 and the given visit_order. */
  Location insert(Location location);

  /**
   * Saves the fields of the location except visit_order if its version is still current.
   *
   * @throws ua.edu.sumdu.traveler.core.error.ConflictException when the stored version differs
   */
  Location update(Location location);

  void deleteById(UUID id);

  /**
   * Gives the locations of the plan the positions 1..n in the order of orderedIds. The uniqueness
   * of (travel_plan_id, visit_order) is checked at commit, so positions may be swapped freely;
   * every moved location gets a new version.
   */
  void reorder(UUID planId, List<UUID> orderedIds);
}
