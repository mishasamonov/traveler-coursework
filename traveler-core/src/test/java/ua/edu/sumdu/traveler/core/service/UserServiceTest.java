package ua.edu.sumdu.traveler.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.edu.sumdu.traveler.core.command.RegistrationCommand;
import ua.edu.sumdu.traveler.core.domain.Actor;
import ua.edu.sumdu.traveler.core.domain.AppUser;
import ua.edu.sumdu.traveler.core.domain.Role;
import ua.edu.sumdu.traveler.core.error.ForbiddenOperationException;
import ua.edu.sumdu.traveler.core.error.ValidationException;
import ua.edu.sumdu.traveler.core.support.InMemoryStore;

class UserServiceTest {

  private InMemoryStore store;
  private UserService service;
  private Actor admin;

  @BeforeEach
  void setUp() {
    store = new InMemoryStore();
    service = new UserService(store.userPort, raw -> "hashed:" + raw);
    AppUser a = store.addUser("admin@test", Role.ADMIN);
    admin = new Actor(a.id(), a.email(), a.role());
  }

  @Test
  @DisplayName("Реєстрація: e-mail у нижньому регістрі, роль USER, пароль лише як хеш")
  void register() {
    AppUser user = service.register(new RegistrationCommand(" Olena@Test.UA ", "Олена", "secret123"));
    assertThat(user.email()).isEqualTo("olena@test.ua");
    assertThat(user.role()).isEqualTo(Role.USER);
    assertThat(user.passwordHash()).isEqualTo("hashed:secret123");
    assertThatThrownBy(() -> service.register(new RegistrationCommand("olena@test.ua", "Інша", "secret123")))
        .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("email");
    assertThatThrownBy(() -> service.register(new RegistrationCommand("x@test.ua", "Ікс", "password")))
        .hasMessageContaining("літери й цифри");
  }

  @Test
  @DisplayName("Адміністратор змінює роль і блокує, але не себе й не останнього адміністратора")
  void adminRules() {
    AppUser user = service.register(new RegistrationCommand("u@test.ua", "Юзер", "secret123"));
    assertThat(service.setEnabled(admin, user.id(), false).enabled()).isFalse();
    assertThatThrownBy(() -> service.setEnabled(admin, admin.userId(), false))
        .isInstanceOf(ValidationException.class);
    assertThatThrownBy(() -> service.changeRole(admin, admin.userId(), Role.USER))
        .isInstanceOf(ValidationException.class);
    AppUser second = service.changeRole(admin, user.id(), Role.ADMIN);
    Actor secondAdmin = new Actor(second.id(), second.email(), Role.ADMIN);
    service.setEnabled(admin, second.id(), true);
    // Two active administrators: one of them may be demoted, then the last one is protected.
    service.changeRole(secondAdmin, admin.userId(), Role.USER);
    Actor former = new Actor(admin.userId(), admin.email(), Role.USER);
    assertThatThrownBy(() -> service.listUsers(former)).isInstanceOf(ForbiddenOperationException.class);
    assertThat(store.userPort.countEnabledByRole(Role.ADMIN)).isEqualTo(1);
  }
}
