package ua.edu.sumdu.traveler.web.security;

import jakarta.servlet.http.HttpServletRequest;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

/** Security audit: sign-ins, failed sign-ins, sign-outs and denied requests go to the log. */
@Component
public class SecurityAuditListener {

  private static final Logger audit = LoggerFactory.getLogger("ua.edu.sumdu.traveler.audit");

  @EventListener
  public void onSuccess(AuthenticationSuccessEvent event) {
    audit.info("LOGIN_SUCCESS user={}", event.getAuthentication().getName());
  }

  @EventListener
  public void onFailure(AbstractAuthenticationFailureEvent event) {
    audit.warn("LOGIN_FAILURE user={} reason={}", event.getAuthentication().getName(),
        event.getException().getClass().getSimpleName());
  }

  @EventListener
  public void onLogout(LogoutSuccessEvent event) {
    audit.info("LOGOUT user={}", event.getAuthentication().getName());
  }

  @EventListener
  public void onDenied(AuthorizationDeniedEvent<?> event) {
    var authentication = event.getAuthentication().get();
    Object object = event.getObject();
    String target = object instanceof HttpServletRequest request
        ? request.getMethod() + " " + request.getRequestURI()
        : object instanceof MethodInvocation invocation
            ? invocation.getMethod().getDeclaringClass().getSimpleName() + "."
                + invocation.getMethod().getName()
            : String.valueOf(object);
    audit.warn("ACCESS_DENIED user={} target={}",
        authentication == null ? "?" : authentication.getName(), target);
  }
}
