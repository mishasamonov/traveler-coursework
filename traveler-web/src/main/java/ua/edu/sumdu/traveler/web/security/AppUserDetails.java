package ua.edu.sumdu.traveler.web.security;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ua.edu.sumdu.traveler.core.domain.Actor;
import ua.edu.sumdu.traveler.core.domain.AppUser;
import ua.edu.sumdu.traveler.core.domain.Role;

/** The signed-in user kept in the HTTP session. The role becomes the authority ROLE_USER/ADMIN. */
public class AppUserDetails implements UserDetails, CredentialsContainer {

  private final UUID id;
  private final String email;
  private final String displayName;
  private final Role role;
  private final boolean enabled;
  private String passwordHash;

  public AppUserDetails(AppUser user) {
    this.id = user.id();
    this.email = user.email();
    this.displayName = user.displayName();
    this.role = user.role();
    this.enabled = user.enabled();
    this.passwordHash = user.passwordHash();
  }

  /** The actor for the business layer. */
  public Actor actor() {
    return new Actor(id, email, role);
  }

  public static Actor actorOf(AppUserDetails user) {
    return user == null ? Actor.GUEST : user.actor();
  }

  public UUID getId() { return id; }
  public String getDisplayName() { return displayName; }
  public Role getRole() { return role; }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override public String getPassword() { return passwordHash; }
  @Override public String getUsername() { return email; }
  @Override public boolean isEnabled() { return enabled; }

  /** The hash is not needed after authentication and is not kept in the session. */
  @Override
  public void eraseCredentials() {
    passwordHash = null;
  }

  /** The session registry finds the sessions of a user by an equal principal. */
  @Override
  public boolean equals(Object o) {
    return o instanceof AppUserDetails other && Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return "AppUserDetails[" + email + ", " + role + "]";
  }
}
