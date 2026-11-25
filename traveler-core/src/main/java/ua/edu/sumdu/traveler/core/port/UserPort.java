package ua.edu.sumdu.traveler.core.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ua.edu.sumdu.traveler.core.domain.AppUser;
import ua.edu.sumdu.traveler.core.domain.Role;

/** Data access to user accounts (table app_users). */
public interface UserPort {

  Optional<AppUser> findById(UUID id);

  /** E-mails are stored in lower case, the lookup ignores case. */
  Optional<AppUser> findByEmail(String email);

  boolean existsByEmail(String email);

  AppUser insert(AppUser user);

  AppUser update(AppUser user);

  List<AppUser> findAll();

  long countEnabledByRole(Role role);
}
