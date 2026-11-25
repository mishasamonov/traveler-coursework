package ua.edu.sumdu.traveler.web.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ua.edu.sumdu.traveler.core.port.PasswordHasher;

/** The port of the core implemented with the PasswordEncoder (BCrypt) of Spring Security. */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

  private final PasswordEncoder encoder;

  public BCryptPasswordHasher(PasswordEncoder encoder) {
    this.encoder = encoder;
  }

  @Override
  public String hash(String rawPassword) {
    return encoder.encode(rawPassword);
  }
}
