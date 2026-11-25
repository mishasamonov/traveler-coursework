package ua.edu.sumdu.traveler.core.domain;

import java.util.UUID;

/**
 * Who performs an operation. The web layer builds it from the Spring Security principal, so the
 * business logic checks permissions without depending on Spring Security.
 */
public record Actor(UUID userId, String email, Role role) {

  public static final Actor GUEST = new Actor(null, null, null);

  public boolean isAuthenticated() {
    return userId != null;
  }

  public boolean isAdmin() {
    return role == Role.ADMIN;
  }

  public boolean owns(TravelPlan plan) {
    return isAuthenticated() && userId.equals(plan.ownerId());
  }

  /** The owner and the administrator may change a plan and its route. */
  public boolean canEdit(TravelPlan plan) {
    return isAdmin() || owns(plan);
  }

  /** Public plans are visible to everybody, private ones only to the owner and the administrator. */
  public boolean canView(TravelPlan plan) {
    return plan.isPublic() || canEdit(plan);
  }
}
