package ua.edu.sumdu.traveler.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.edu.sumdu.traveler.core.domain.Role;
import ua.edu.sumdu.traveler.persistence.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

  Optional<UserEntity> findByEmail(String email);

  boolean existsByEmail(String email);

  long countByRoleAndEnabledTrue(Role role);

  long countByRole(Role role);

  long countByEnabledFalse();

  List<UserEntity> findAllByOrderByCreatedAtAsc();
}
