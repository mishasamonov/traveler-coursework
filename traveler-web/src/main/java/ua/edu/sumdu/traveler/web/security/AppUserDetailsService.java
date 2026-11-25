package ua.edu.sumdu.traveler.web.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ua.edu.sumdu.traveler.core.service.UserService;

/** Loads accounts for form login: the login is the e-mail, the password is checked with BCrypt. */
@Service
public class AppUserDetailsService implements UserDetailsService {

  private final UserService users;

  public AppUserDetailsService(UserService users) {
    this.users = users;
  }

  @Override
  public UserDetails loadUserByUsername(String email) {
    return users.findByEmail(email).map(AppUserDetails::new)
        .orElseThrow(() -> new UsernameNotFoundException("Unknown e-mail"));
  }
}
