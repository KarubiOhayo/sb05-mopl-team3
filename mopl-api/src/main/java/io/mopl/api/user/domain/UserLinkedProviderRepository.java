package io.mopl.api.user.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLinkedProviderRepository extends JpaRepository<UserLinkedProvider, UUID> {

  /** 사용자의 모든 연동 계정 조회 */
  List<UserLinkedProvider> findByUserId(UUID userId);

  /** 사용자의 특정 제공자 연동 계정 조회 */
  Optional<UserLinkedProvider> findByUserIdAndProvider(UUID userId, AuthProvider provider);

  /** 제공자와 제공자의 사용자 ID로 연동 계정 조회 */
  Optional<UserLinkedProvider> findByProviderAndProviderUserId(
      AuthProvider provider, String providerUserId);

  /** 사용자의 특정 제공자 연동 여부 확인 */
  boolean existsByUserIdAndProvider(UUID userId, AuthProvider provider);

  /** 사용자의 연동 계정 개수 조회 */
  long countByUserId(UUID userId);

  /** 사용자의 특정 제공자 연동 계정 삭제 */
  void deleteByUserIdAndProvider(UUID userId, AuthProvider provider);
}
