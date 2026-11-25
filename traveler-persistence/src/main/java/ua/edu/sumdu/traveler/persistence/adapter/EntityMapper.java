package ua.edu.sumdu.traveler.persistence.adapter;

import ua.edu.sumdu.traveler.core.domain.AppUser;
import ua.edu.sumdu.traveler.core.domain.Location;
import ua.edu.sumdu.traveler.core.domain.TravelPlan;
import ua.edu.sumdu.traveler.persistence.entity.LocationEntity;
import ua.edu.sumdu.traveler.persistence.entity.TravelPlanEntity;
import ua.edu.sumdu.traveler.persistence.entity.UserEntity;

/** Converts JPA entities to the domain records of the core module. */
final class EntityMapper {

  private EntityMapper() {}

  static TravelPlan toDomain(TravelPlanEntity e) {
    return new TravelPlan(e.getId(), e.getOwner() == null ? null : e.getOwner().getId(),
        e.getTitle(), e.getDescription(), e.getStartDate(), e.getEndDate(), e.getBudget(),
        e.getCurrency(), e.isPublicPlan(), e.getVersion(), e.getCreatedAt(), e.getUpdatedAt());
  }

  static Location toDomain(LocationEntity e) {
    return new Location(e.getId(), e.getTravelPlanId(), e.getName(), e.getAddress(),
        e.getLatitude(), e.getLongitude(), e.getVisitOrder(), e.getArrivalDate(),
        e.getDepartureDate(), e.getBudget(), e.getNotes(), e.getVersion(), e.getCreatedAt());
  }

  static AppUser toDomain(UserEntity e) {
    return new AppUser(e.getId(), e.getEmail(), e.getDisplayName(), e.getPasswordHash(),
        e.getRole(), e.isEnabled(), e.getCreatedAt());
  }

  /** Copies the editable fields of a plan; the owner and the version are handled by the caller. */
  static void copy(TravelPlan from, TravelPlanEntity to) {
    to.setTitle(from.title());
    to.setDescription(from.description());
    to.setStartDate(from.startDate());
    to.setEndDate(from.endDate());
    to.setBudget(from.budget());
    to.setCurrency(from.currency());
    to.setPublicPlan(from.isPublic());
  }

  /** Copies the editable fields of a location; visit_order is changed only by reorder(). */
  static void copy(Location from, LocationEntity to) {
    to.setName(from.name());
    to.setAddress(from.address());
    to.setLatitude(from.latitude());
    to.setLongitude(from.longitude());
    to.setArrivalDate(from.arrivalDate());
    to.setDepartureDate(from.departureDate());
    to.setBudget(from.budget());
    to.setNotes(from.notes());
  }
}
