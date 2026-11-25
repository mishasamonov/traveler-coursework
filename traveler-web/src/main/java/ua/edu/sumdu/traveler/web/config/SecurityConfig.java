package ua.edu.sumdu.traveler.web.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Authentication with a login form (e-mail + password, BCrypt) and authorization by roles:
 * a guest reads the public catalogue, USER manages own plans, ADMIN manages everything plus
 * accounts and Actuator. Fine-grained rules (whose plan it is) are checked in the core services.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** Tracks sessions so that the administrator can end the sessions of a blocked user. */
  @Bean
  public SessionRegistry sessionRegistry() {
    return new SessionRegistryImpl();
  }

  @Bean
  public HttpSessionEventPublisher httpSessionEventPublisher() {
    return new HttpSessionEventPublisher();
  }

  /** Publishes AuthorizationDeniedEvent for the security audit log. */
  @Bean
  public AuthorizationEventPublisher authorizationEventPublisher(ApplicationEventPublisher events) {
    return new SpringAuthorizationEventPublisher(events);
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, SessionRegistry sessions)
      throws Exception {
    ExceptionMappingAuthenticationFailureHandler failure =
        new ExceptionMappingAuthenticationFailureHandler();
    failure.setDefaultFailureUrl("/login?error");
    failure.setExceptionMappings(java.util.Map.of(DisabledException.class.getName(), "/login?blocked"));

    http.authorizeHttpRequests(auth -> auth
            // Static resources and error pages.
            .requestMatchers("/css/**", "/js/**", "/img/**", "/webjars/**", "/favicon.ico").permitAll()
            .requestMatchers("/error", "/error/**").permitAll()
            // Actuator: health and info are public (monitoring probes), the rest is for ADMIN.
            .requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class)).permitAll()
            .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ADMIN")
            // Administration.
            .requestMatchers("/admin/**").hasRole("ADMIN")
            // Guest: home, public catalogue, a plan page (the service hides private plans).
            .requestMatchers("/plans/new").authenticated()
            .requestMatchers(org.springframework.http.HttpMethod.GET, "/", "/plans", "/plans/{id}")
                .permitAll()
            .requestMatchers("/login", "/register").permitAll()
            // Everything else (my plans, editing, routes) needs a signed-in user.
            .anyRequest().hasAnyRole("USER", "ADMIN"))
        .formLogin(form -> form
            .loginPage("/login")
            .usernameParameter("email")
            .passwordParameter("password")
            .defaultSuccessUrl("/my/plans")
            .failureHandler(failure)
            .permitAll())
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/?logout")
            .deleteCookies("JSESSIONID"))
        // After login the user returns to the page that required it, without the "?continue" marker.
        .requestCache(cache -> cache.requestCache(savedRequests()))
        .sessionManagement(session -> session
            .sessionFixation(fixation -> fixation.changeSessionId())
            .maximumSessions(-1)
            .sessionRegistry(sessions)
            .expiredUrl("/login?expired"));
    return http.build();
  }

  private static HttpSessionRequestCache savedRequests() {
    HttpSessionRequestCache cache = new HttpSessionRequestCache();
    cache.setMatchingRequestParameterName(null);
    return cache;
  }
}
