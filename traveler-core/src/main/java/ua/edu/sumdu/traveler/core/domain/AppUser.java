package ua.edu.sumdu.traveler.core.domain;

import java.time.Instant;
import java.util.UUID;

/** Account of the information system (table app_users). */
public record AppUser(
    UUID id,
    String email,
    String displayName,
    String passwordHash,
    Role role,
    boolean enabled,
    Instant createdAt) {

  public boolean isAdmin() {
    return role == Role.ADMIN;
  }

  public AppUser withRole(Role newRole) {
    return new AppUser(id, email, displayName, passwordHash, newRole, enabled, createdAt);
  }

  public AppUser withEnabled(boolean value) {
    return new AppUser(id, email, displayName, passwordHash, role, value, createdAt);
  }
}
