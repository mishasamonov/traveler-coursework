package ua.edu.sumdu.traveler.web.security;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

/**
 * Ends the active sessions of a user whom the administrator blocked or whose role changed: the
 * next request of that user goes to the login page, so the change takes effect at once.
 */
@Component
public class SessionTerminator {

  private static final Logger log = LoggerFactory.getLogger(SessionTerminator.class);

  private final SessionRegistry registry;

  public SessionTerminator(SessionRegistry registry) {
    this.registry = registry;
  }

  public int expireSessionsOf(UUID userId) {
    int count = 0;
    for (Object principal : registry.getAllPrincipals()) {
      if (principal instanceof AppUserDetails user && user.getId().equals(userId)) {
        for (SessionInformation session : registry.getAllSessions(principal, false)) {
          session.expireNow();
          count++;
        }
      }
    }
    if (count > 0) log.info("Expired {} session(s) of user {}", count, userId);
    return count;
  }
}
