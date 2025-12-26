package io.mopl.api.user.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  // 소셜 로그인용 (심화)
  Optional<User> findByAuthProviderAndProviderUserId(
      AuthProvider authProvider,
      String providerUserId
  );

}
