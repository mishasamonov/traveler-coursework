package ua.edu.sumdu.traveler.core.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.sumdu.traveler.core.command.RegistrationCommand;
import ua.edu.sumdu.traveler.core.domain.Actor;
import ua.edu.sumdu.traveler.core.domain.AppUser;
import ua.edu.sumdu.traveler.core.domain.Role;
import ua.edu.sumdu.traveler.core.error.NotFoundException;
import ua.edu.sumdu.traveler.core.error.ValidationException;
import ua.edu.sumdu.traveler.core.port.PasswordHasher;
import ua.edu.sumdu.traveler.core.port.UserPort;

/**
 * Accounts: self-registration of travellers and the administrator's control over users (role,
 * blocking). The system always keeps at least one active administrator.
 */
@Transactional
public class UserService {

  private static final Logger log = LoggerFactory.getLogger(UserService.class);
  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
  private static final Pattern LETTER = Pattern.compile("\\p{L}");
  private static final Pattern DIGIT = Pattern.compile("\\d");

  private final UserPort users;
  private final PasswordHasher hasher;

  public UserService(UserPort users, PasswordHasher hasher) {
    this.users = users;
    this.hasher = hasher;
  }

  public AppUser register(RegistrationCommand command) {
    String email = normalizeEmail(command.email());
    String name = InputRules.required(command.displayName(), "displayName", "Ім'я", 100);
    if (name.length() < 2) throw new ValidationException("displayName", "Ім'я — щонайменше 2 символи");
    String password = command.password() == null ? "" : command.password();
    if (password.length() < 8 || password.length() > 72) {
      throw new ValidationException("password", "Пароль має містити від 8 до 72 символів");
    }
    if (!LETTER.matcher(password).find() || !DIGIT.matcher(password).find()) {
      throw new ValidationException("password", "Пароль має містити літери й цифри");
    }
    if (users.existsByEmail(email)) {
      throw new ValidationException("email", "Користувач з такою адресою вже зареєстрований");
    }
    AppUser saved = users.insert(new AppUser(null, email, name, hasher.hash(password), Role.USER,
        true, null));
    log.info("User registered: id={}, email={}", saved.id(), saved.email());
    return saved;
  }

  /** Used by the authentication of the web layer. */
  @Transactional(readOnly = true)
  public Optional<AppUser> findByEmail(String email) {
    if (email == null || email.isBlank()) return Optional.empty();
    return users.findByEmail(email.strip().toLowerCase(Locale.ROOT));
  }

  @Transactional(readOnly = true)
  public List<AppUser> listUsers(Actor actor) {
    TravelPlanService.requireAdmin(actor);
    return users.findAll();
  }

  public AppUser changeRole(Actor actor, UUID userId, Role role) {
    TravelPlanService.requireAdmin(actor);
    AppUser user = find(userId);
    if (user.id().equals(actor.userId())) {
      throw new ValidationException("Не можна змінити власну роль");
    }
    if (user.role() == role) return user;
    if (user.isAdmin() && user.enabled()) keepAnotherAdmin();
    AppUser saved = users.update(user.withRole(role));
    log.info("Role changed: user={}, {} -> {}, by={}", user.email(), user.role(), role, actor.email());
    return saved;
  }

  /** Blocks (enabled = false) or unblocks an account; a blocked user cannot sign in. */
  public AppUser setEnabled(Actor actor, UUID userId, boolean enabled) {
    TravelPlanService.requireAdmin(actor);
    AppUser user = find(userId);
    if (user.id().equals(actor.userId())) {
      throw new ValidationException("Не можна заблокувати власний обліковий запис");
    }
    if (user.enabled() == enabled) return user;
    if (!enabled && user.isAdmin()) keepAnotherAdmin();
    AppUser saved = users.update(user.withEnabled(enabled));
    log.info("User {}: {}, by={}", enabled ? "unblocked" : "blocked", user.email(), actor.email());
    return saved;
  }

  private void keepAnotherAdmin() {
    if (users.countEnabledByRole(Role.ADMIN) <= 1) {
      throw new ValidationException("У системі має залишитися хоча б один активний адміністратор");
    }
  }

  private AppUser find(UUID id) {
    return users.findById(id).orElseThrow(() -> new NotFoundException("Користувача не знайдено"));
  }

  static String normalizeEmail(String value) {
    String email = InputRules.required(value, "email", "Електронна пошта", 254)
        .toLowerCase(Locale.ROOT);
    if (!EMAIL.matcher(email).matches()) {
      throw new ValidationException("email", "Некоректна адреса електронної пошти");
    }
    return email;
  }
}
