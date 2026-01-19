package io.mopl.api.user.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  long countByRole(UserRole userRole);

  Optional<User> findByAuthProviderAndProviderUserId(
      AuthProvider authProvider, String providerUserId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.id = :id")
  Optional<User> findByIdWithLock(@Param("id") UUID id);
}
