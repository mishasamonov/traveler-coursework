package ua.edu.sumdu.traveler.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.edu.sumdu.traveler.persistence.entity.LocationEntity;

public interface LocationRepository extends JpaRepository<LocationEntity, UUID> {

  List<LocationEntity> findByTravelPlanIdOrderByVisitOrderAsc(UUID travelPlanId);

  @Query("select coalesce(max(l.visitOrder), 0) from LocationEntity l "
      + "where l.travelPlanId = :planId")
  int maxVisitOrder(@Param("planId") UUID planId);

  /** Rows: plan id, number of locations, sum of planned expenses. */
  @Query("select l.travelPlanId, count(l), coalesce(sum(l.budget), 0) from LocationEntity l "
      + "where l.travelPlanId in :planIds group by l.travelPlanId")
  List<Object[]> aggregateByPlanIds(@Param("planIds") Collection<UUID> planIds);

  /**
   * UNIQUE (travel_plan_id, visit_order) is DEFERRABLE INITIALLY IMMEDIATE (the only deferrable
   * constraint of the schema): in this transaction it is checked at commit, not per row.
   */
  @Modifying
  @Query(value = "SET CONSTRAINTS ALL DEFERRED", nativeQuery = true)
  void deferOrderConstraint();

  /** Rows: location name, number of plans that visit it. */
  @Query(value = "select name, count(distinct travel_plan_id) as plans from locations "
      + "group by name order by plans desc, name limit :limit", nativeQuery = true)
  List<Object[]> topPlaces(@Param("limit") int limit);

  /** Plans whose positions are not 1..n (possible after deletions through the REST API). */
  @Query(value = "select count(*) from (select travel_plan_id from locations "
      + "group by travel_plan_id having max(visit_order) <> count(*)) gaps", nativeQuery = true)
  long countPlansWithOrderGaps();
}
