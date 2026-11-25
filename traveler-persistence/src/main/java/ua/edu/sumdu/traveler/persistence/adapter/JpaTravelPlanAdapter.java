package ua.edu.sumdu.traveler.persistence.adapter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import ua.edu.sumdu.traveler.core.domain.PageResult;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria;
import ua.edu.sumdu.traveler.core.domain.PlanStatus;
import ua.edu.sumdu.traveler.core.domain.PlanSummary;
import ua.edu.sumdu.traveler.core.domain.Role;
import ua.edu.sumdu.traveler.core.domain.TravelPlan;
import ua.edu.sumdu.traveler.core.domain.TravelStatistics;
import ua.edu.sumdu.traveler.core.domain.TravelStatistics.NamedCount;
import ua.edu.sumdu.traveler.core.error.ConflictException;
import ua.edu.sumdu.traveler.core.error.NotFoundException;
import ua.edu.sumdu.traveler.core.port.TravelPlanPort;
import ua.edu.sumdu.traveler.persistence.entity.TravelPlanEntity;
import ua.edu.sumdu.traveler.persistence.entity.UserEntity;
import ua.edu.sumdu.traveler.persistence.repository.LocationRepository;
import ua.edu.sumdu.traveler.persistence.repository.TravelPlanRepository;
import ua.edu.sumdu.traveler.persistence.repository.UserRepository;

/** TravelPlanPort on Spring Data JPA. Transactions are opened by the core services. */
@Repository
public class JpaTravelPlanAdapter implements TravelPlanPort {

  @PersistenceContext
  private EntityManager em;

  private final TravelPlanRepository plans;
  private final LocationRepository locations;
  private final UserRepository users;

  public JpaTravelPlanAdapter(TravelPlanRepository plans, LocationRepository locations,
      UserRepository users) {
    this.plans = plans;
    this.locations = locations;
    this.users = users;
  }

  @Override
  public Optional<TravelPlan> findById(UUID id) {
    return plans.findById(id).map(EntityMapper::toDomain);
  }

  @Override
  public Optional<TravelPlan> lockById(UUID id) {
    return plans.findByIdForUpdate(id).map(EntityMapper::toDomain);
  }

  @Override
  public TravelPlan insert(TravelPlan plan) {
    TravelPlanEntity entity = new TravelPlanEntity();
    EntityMapper.copy(plan, entity);
    entity.setOwner(plan.ownerId() == null ? null : em.getReference(UserEntity.class, plan.ownerId()));
    entity.setVersion(1); // the laboratory schema requires version > 0
    em.persist(entity);
    em.flush();
    return EntityMapper.toDomain(entity);
  }

  @Override
  public TravelPlan update(TravelPlan plan) {
    TravelPlanEntity entity = plans.findById(plan.id())
        .orElseThrow(() -> new NotFoundException("План подорожі не знайдено"));
    if (!entity.getVersion().equals(plan.version())) {
      throw conflict(entity.getVersion());
    }
    EntityMapper.copy(plan, entity);
    try {
      em.flush(); // UPDATE ... WHERE id = ? AND version = ?
    } catch (OptimisticLockException e) {
      throw conflict(null);
    }
    return EntityMapper.toDomain(entity);
  }

  @Override
  public void deleteById(UUID id) {
    plans.findById(id).ifPresent(entity -> {
      em.remove(entity);
      em.flush();
    });
  }

  @Override
  public PageResult<PlanSummary> search(PlanSearchCriteria c) {
    Page<TravelPlanEntity> page = plans.findAll(PlanSpecifications.matching(c),
        PageRequest.of(c.page(), c.size(), PlanSpecifications.sort(c.sort())));
    Map<UUID, Object[]> totals = new HashMap<>();
    List<UUID> ids = page.getContent().stream().map(TravelPlanEntity::getId).toList();
    if (!ids.isEmpty()) {
      for (Object[] row : locations.aggregateByPlanIds(ids)) totals.put((UUID) row[0], row);
    }
    List<PlanSummary> content = page.getContent().stream().map(p -> {
      Object[] row = totals.get(p.getId());
      long count = row == null ? 0 : ((Number) row[1]).longValue();
      BigDecimal spend = row == null ? BigDecimal.ZERO : toMoney(row[2]);
      UserEntity owner = p.getOwner();
      return new PlanSummary(p.getId(), p.getTitle(), p.getDescription(), p.getStartDate(),
          p.getEndDate(), p.getBudget(), p.getCurrency(), p.isPublicPlan(),
          owner == null ? null : owner.getId(), owner == null ? null : owner.getDisplayName(),
          count, spend.setScale(2, RoundingMode.HALF_UP), p.getUpdatedAt(),
          PlanStatus.of(p.getStartDate(), p.getEndDate(), c.today()));
    }).toList();
    return new PageResult<>(content, c.page(), c.size(), page.getTotalElements());
  }

  @Override
  public TravelStatistics statistics() {
    long planCount = plans.count();
    long locationCount = locations.count();
    Map<String, BigDecimal> budgets = new LinkedHashMap<>();
    for (Object[] row : plans.averageBudgetByCurrency()) {
      budgets.put((String) row[0], toMoney(row[1]).setScale(2, RoundingMode.HALF_UP));
    }
    List<NamedCount> places = locations.topPlaces(5).stream()
        .map(r -> new NamedCount((String) r[0], ((Number) r[1]).longValue())).toList();
    double average = planCount == 0 ? 0
        : BigDecimal.valueOf(locationCount).divide(BigDecimal.valueOf(planCount), 2,
            RoundingMode.HALF_UP).doubleValue();
    return new TravelStatistics(users.count(), users.countByRole(Role.ADMIN),
        users.countByEnabledFalse(), planCount, plans.countByPublicPlanTrue(),
        plans.countByOwnerIsNull(), locationCount, average, budgets, places,
        locations.countPlansWithOrderGaps());
  }

  private static BigDecimal toMoney(Object value) {
    if (value == null) return BigDecimal.ZERO;
    if (value instanceof BigDecimal b) return b;
    return BigDecimal.valueOf(((Number) value).doubleValue());
  }

  static ConflictException conflict(Integer current) {
    return new ConflictException("Запис уже змінено в іншому вікні або іншим користувачем. "
        + "Оновіть сторінку й повторіть.", current);
  }
}
