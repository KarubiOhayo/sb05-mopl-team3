package io.mopl.api.user.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  long countByRole(UserRole userRole);

  // 소셜 로그인용 (심화)
  Optional<User> findByAuthProviderAndProviderUserId(
      AuthProvider authProvider, String providerUserId);

  //  @Query(
  //      "UPDATE User u SET u.tempPasswordHash = null, u.tempPasswordExpiresAt = null "
  //          + "WHERE u.tempPasswordHash IS NOT NULL AND u.tempPasswordExpiresAt <= :now")
  //  int clearExpiredTempPasswords(@Param("now") Instant now);
}
