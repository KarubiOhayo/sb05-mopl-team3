package io.mopl.api.user.service;

import io.mopl.api.common.error.AuthErrorCode;
import io.mopl.api.common.error.UserErrorCode;
import io.mopl.api.common.util.EmailMaskingUtils;
import io.mopl.api.user.domain.AuthProvider;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserLinkedProvider;
import io.mopl.api.user.domain.UserLinkedProviderRepository;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.dto.LinkedProviderDto;
import io.mopl.core.error.BusinessException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserLinkedProviderService {

  private final UserRepository userRepository;
  private final UserLinkedProviderRepository linkedProviderRepository;

  /** 사용자의 연동된 소셜 제공자 목록 조회 (마스킹된 이메일 포함) */
  @Transactional(readOnly = true)
  public List<LinkedProviderDto> getLinkedProviders(UUID userId) {
    return linkedProviderRepository.findByUserId(userId).stream()
        .map(
            link -> {
              String maskedEmail = EmailMaskingUtils.maskEmail(link.getProviderEmail());
              return LinkedProviderDto.of(link.getProvider(), maskedEmail, link.getLinkedAt());
            })
        .collect(Collectors.toList());
  }

  /** 소셜 제공자 연동 추가 */
  @Transactional
  public void linkProvider(
      UUID userId, AuthProvider provider, String providerUserId, String providerEmail) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    if (linkedProviderRepository.existsByUserIdAndProvider(userId, provider)) {
      throw new BusinessException(AuthErrorCode.PROVIDER_ALREADY_LINKED);
    }

    linkedProviderRepository
        .findByProviderAndProviderUserId(provider, providerUserId)
        .ifPresent(
            existing -> {
              if (!existing.getUserId().equals(userId)) {
                throw new BusinessException(AuthErrorCode.PROVIDER_ALREADY_LINKED_TO_ANOTHER_USER);
              }
            });

    if (providerEmail != null) {
      userRepository
          .findByEmail(providerEmail)
          .ifPresent(
              existingUser -> {
                if (!existingUser.getId().equals(userId)) {
                  throw new BusinessException(AuthErrorCode.OAUTH2_EMAIL_ALREADY_REGISTERED);
                }
              });
    }

    UserLinkedProvider link =
        UserLinkedProvider.create(userId, provider, providerUserId, providerEmail);
    linkedProviderRepository.save(link);
  }

  /** 소셜 제공자 연동 해제 */
  @Transactional
  public void unlinkProvider(UUID userId, AuthProvider provider) {
    User user =
        userRepository
            .findByIdWithLock(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    if (!linkedProviderRepository.existsByUserIdAndProvider(userId, provider)) {
      throw new BusinessException(AuthErrorCode.PROVIDER_NOT_LINKED);
    }

    if (user.getAuthProvider() == provider && user.getAuthProvider() != AuthProvider.LOCAL) {
      throw new BusinessException(AuthErrorCode.CANNOT_UNLINK_INITIAL_PROVIDER);
    }

    long linkedCount = linkedProviderRepository.countByUserId(userId);
    boolean hasLocalAccount = user.getAuthProvider() == AuthProvider.LOCAL;

    if (linkedCount == 1 && !hasLocalAccount) {
      throw new BusinessException(AuthErrorCode.CANNOT_UNLINK_LAST_LOGIN_METHOD);
    }

    linkedProviderRepository.deleteByUserIdAndProvider(userId, provider);
  }

  /** 제공자와 제공자의 사용자 ID로 연동 정보 조회 */
  @Transactional(readOnly = true)
  public UserLinkedProvider findByProviderAndProviderUserId(
      AuthProvider provider, String providerUserId) {
    return linkedProviderRepository
        .findByProviderAndProviderUserId(provider, providerUserId)
        .orElse(null);
  }
}
