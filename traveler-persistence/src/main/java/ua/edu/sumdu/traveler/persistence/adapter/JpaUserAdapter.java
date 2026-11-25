package ua.edu.sumdu.traveler.persistence.adapter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import ua.edu.sumdu.traveler.core.domain.AppUser;
import ua.edu.sumdu.traveler.core.domain.Role;
import ua.edu.sumdu.traveler.core.error.NotFoundException;
import ua.edu.sumdu.traveler.core.port.UserPort;
import ua.edu.sumdu.traveler.persistence.entity.UserEntity;
import ua.edu.sumdu.traveler.persistence.repository.UserRepository;

/** UserPort on Spring Data JPA. */
@Repository
public class JpaUserAdapter implements UserPort {

  @PersistenceContext
  private EntityManager em;

  private final UserRepository users;

  public JpaUserAdapter(UserRepository users) {
    this.users = users;
  }

  @Override
  public Optional<AppUser> findById(UUID id) {
    return users.findById(id).map(EntityMapper::toDomain);
  }

  @Override
  public Optional<AppUser> findByEmail(String email) {
    return users.findByEmail(email).map(EntityMapper::toDomain);
  }

  @Override
  public boolean existsByEmail(String email) {
    return users.existsByEmail(email);
  }

  @Override
  public AppUser insert(AppUser user) {
    UserEntity entity = new UserEntity();
    entity.setEmail(user.email());
    entity.setDisplayName(user.displayName());
    entity.setPasswordHash(user.passwordHash());
    entity.setRole(user.role());
    entity.setEnabled(user.enabled());
    em.persist(entity);
    em.flush();
    return EntityMapper.toDomain(entity);
  }

  @Override
  public AppUser update(AppUser user) {
    UserEntity entity = users.findById(user.id())
        .orElseThrow(() -> new NotFoundException("Користувача не знайдено"));
    entity.setDisplayName(user.displayName());
    entity.setRole(user.role());
    entity.setEnabled(user.enabled());
    em.flush();
    return EntityMapper.toDomain(entity);
  }

  @Override
  public List<AppUser> findAll() {
    return users.findAllByOrderByCreatedAtAsc().stream().map(EntityMapper::toDomain).toList();
  }

  @Override
  public long countEnabledByRole(Role role) {
    return users.countByRoleAndEnabledTrue(role);
  }
}
