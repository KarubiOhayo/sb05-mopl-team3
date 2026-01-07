package io.mopl.api.user.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  // 소셜 로그인용 (심화)
  Optional<User> findByAuthProviderAndProviderUserId(
      AuthProvider authProvider, String providerUserId);

  /** 만료된 임시 비밀번호 일괄 삭제 */
  @Modifying
  @Query(
      "UPDATE User u SET u.tempPasswordHash = null, u.tempPasswordExpiresAt = null "
          + "WHERE u.tempPasswordHash IS NOT NULL AND u.tempPasswordExpiresAt <= :now")
  int clearExpiredTempPasswords(@Param("now") Instant now);

  String role(UserRole role);
}
