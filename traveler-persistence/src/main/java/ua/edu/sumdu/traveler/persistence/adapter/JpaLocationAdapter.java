package ua.edu.sumdu.traveler.persistence.adapter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import ua.edu.sumdu.traveler.core.domain.Location;
import ua.edu.sumdu.traveler.core.error.ConflictException;
import ua.edu.sumdu.traveler.core.error.NotFoundException;
import ua.edu.sumdu.traveler.core.port.LocationPort;
import ua.edu.sumdu.traveler.persistence.entity.LocationEntity;
import ua.edu.sumdu.traveler.persistence.repository.LocationRepository;

/** LocationPort on Spring Data JPA. */
@Repository
public class JpaLocationAdapter implements LocationPort {

  @PersistenceContext
  private EntityManager em;

  private final LocationRepository locations;

  public JpaLocationAdapter(LocationRepository locations) {
    this.locations = locations;
  }

  @Override
  public List<Location> findByPlanId(UUID planId) {
    return locations.findByTravelPlanIdOrderByVisitOrderAsc(planId).stream()
        .map(EntityMapper::toDomain).toList();
  }

  @Override
  public Optional<Location> findById(UUID id) {
    return locations.findById(id).map(EntityMapper::toDomain);
  }

  @Override
  public int maxVisitOrder(UUID planId) {
    return locations.maxVisitOrder(planId);
  }

  @Override
  public Location insert(Location location) {
    LocationEntity entity = new LocationEntity();
    entity.setTravelPlanId(location.travelPlanId());
    entity.setVisitOrder(location.visitOrder());
    EntityMapper.copy(location, entity);
    entity.setVersion(1);
    em.persist(entity);
    em.flush();
    return EntityMapper.toDomain(entity);
  }

  @Override
  public Location update(Location location) {
    LocationEntity entity = locations.findById(location.id())
        .orElseThrow(() -> new NotFoundException("Локацію не знайдено"));
    if (!entity.getVersion().equals(location.version())) {
      throw JpaTravelPlanAdapter.conflict(entity.getVersion());
    }
    EntityMapper.copy(location, entity);
    try {
      em.flush();
    } catch (OptimisticLockException e) {
      throw JpaTravelPlanAdapter.conflict(null);
    }
    return EntityMapper.toDomain(entity);
  }

  @Override
  public void deleteById(UUID id) {
    locations.findById(id).ifPresent(entity -> {
      em.remove(entity);
      em.flush();
    });
  }

  @Override
  public void reorder(UUID planId, List<UUID> orderedIds) {
    List<LocationEntity> route = locations.findByTravelPlanIdOrderByVisitOrderAsc(planId);
    if (route.size() != orderedIds.size()
        || !new HashSet<>(orderedIds).equals(new HashSet<>(route.stream().map(LocationEntity::getId).toList()))) {
      throw new ConflictException("Маршрут уже змінено. Оновіть сторінку й повторіть.", null);
    }
    locations.deferOrderConstraint();
    for (LocationEntity entity : route) {
      int position = orderedIds.indexOf(entity.getId()) + 1;
      if (entity.getVisitOrder() != position) entity.setVisitOrder(position); // version + 1 at flush
    }
    em.flush();
  }
}
