package ua.edu.sumdu.traveler.persistence.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.edu.sumdu.traveler.persistence.entity.TravelPlanEntity;

public interface TravelPlanRepository
    extends JpaRepository<TravelPlanEntity, UUID>, JpaSpecificationExecutor<TravelPlanEntity> {

  /** SELECT ... FOR UPDATE: serializes all route changes of one plan. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from TravelPlanEntity p where p.id = :id")
  Optional<TravelPlanEntity> findByIdForUpdate(@Param("id") UUID id);

  /** Search with filters; the owner is fetched in the same query (no N+1 selects). */
  @Override
  @EntityGraph(attributePaths = "owner")
  Page<TravelPlanEntity> findAll(Specification<TravelPlanEntity> spec, Pageable pageable);

  long countByPublicPlanTrue();

  long countByOwnerIsNull();

  /** Rows: currency, average budget. */
  @Query("select p.currency, avg(p.budget) from TravelPlanEntity p where p.budget is not null "
      + "group by p.currency order by count(p) desc, p.currency")
  List<Object[]> averageBudgetByCurrency();
}
