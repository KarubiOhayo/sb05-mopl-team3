package io.mopl.api.user.service;

import io.mopl.api.auth.service.RefreshTokenService;
import io.mopl.api.common.error.UserErrorCode;
import io.mopl.api.user.domain.AuthProvider;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.domain.UserRole;
import io.mopl.api.user.dto.ChangePasswordRequest;
import io.mopl.api.user.dto.UserCreateRequest;
import io.mopl.api.user.dto.UserDto;
import io.mopl.api.user.dto.UserLockUpdateRequest;
import io.mopl.api.user.dto.UserSummary;
import io.mopl.api.user.dto.UserUpdateRequest;
import io.mopl.core.error.BusinessException;
import io.mopl.redis.constants.RedisKeyPrefix;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final ProfileImageUploadService profileImageUploadService;
  private final RefreshTokenService refreshTokenService;
  private final RedisTemplate<String, String> redisTemplate;

  /** 회원가입 */
  @Transactional
  public UserDto createUser(UserCreateRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new BusinessException(UserErrorCode.DUPLICATED_EMAIL);
    }

    try {
      User user =
          User.builder()
              .email(request.getEmail())
              .name(request.getName())
              .passwordHash(passwordEncoder.encode(request.getPassword()))
              .role(UserRole.USER)
              .authProvider(AuthProvider.LOCAL)
              .locked(false)
              .build();

      User savedUser = userRepository.save(user);

      return UserDto.from(savedUser);

    } catch (DataIntegrityViolationException e) {
      throw new BusinessException(UserErrorCode.DUPLICATED_EMAIL);
    }
  }

  /** 사용자 상세 조회 */
  @Transactional(readOnly = true)
  public UserDto getUserDetails(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    return UserDto.from(user);
  }

  /** 사용자 확인 */
  @Transactional(readOnly = true)
  public UserSummary getUserSummary(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    return UserSummary.builder()
        .userId(user.getId())
        .name(user.getName())
        .profileImageUrl(user.getProfileImageUrl())
        .build();
  }

  /** 비밀번호 변경 */
  @Transactional
  public void changePassword(UUID userId, ChangePasswordRequest request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    if (user.getAuthProvider() != AuthProvider.LOCAL) {
      throw new BusinessException(UserErrorCode.OAUTH_USER_CANNOT_CHANGE_PASSWORD);
    }

    String encodedPassword = passwordEncoder.encode(request.getPassword());
    user.setPasswordHash(encodedPassword);

    user.setTempPasswordHash(null);
    user.setTempPasswordExpiresAt(null);
  }

  /** 프로필 변경 */
  @Transactional
  public UserDto updateProfile(UUID userId, UserUpdateRequest request, MultipartFile profileImage) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    if (request.getName() != null && !request.getName().isBlank()) {
      user.setName(request.getName().trim());
    }

    if (profileImage != null && !profileImage.isEmpty()) {
      String oldImageUrl = user.getProfileImageUrl();

      String newImageUrl = profileImageUploadService.uploadProfileImage(profileImage, userId);
      user.setProfileImageUrl(newImageUrl);
      if (oldImageUrl != null) {
        try {
          profileImageUploadService.deleteImageByUrl(oldImageUrl);
        } catch (Exception e) {
          log.warn("기존 프로필 이미지 삭제 실패: {}", oldImageUrl, e);
        }
      }
    }

    User savedUser = userRepository.save(user);

    return UserDto.from(savedUser);
  }

  /** 계정 잠금 상태 변경 */
  @Transactional
  public void lockUser(UUID userId, UserLockUpdateRequest request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    user.setLocked(request.getLocked());

    String redisKey = RedisKeyPrefix.USER_LOCKED + userId;
    boolean redisSuccess = deleteRedisKeyWithRetry(redisKey, 3);

    if (!redisSuccess) {
      log.error("@@@ CRITICAL: Redis 캐시 삭제 실패 (3회 재시도)");
    }

    if (Boolean.TRUE.equals(request.getLocked())) {
      try {
        refreshTokenService.deleteRefreshToken(userId);
      } catch (Exception e) {
        log.error("Refresh Token 삭제 실패");
      }
    }
  }

  /** Redis 키 삭제 (재시도 로직 포함) */
  private boolean deleteRedisKeyWithRetry(String redisKey, int maxAttempts) {
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        redisTemplate.delete(redisKey);
        return true;
      } catch (Exception e) {
        if (attempt == maxAttempts) {
          log.error("Redis Key 삭제 최종 실패: key = {}", redisKey);
          return false;
        }
        try {
          Thread.sleep(100 * attempt);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          log.error("Redis Key 삭제 재시도 중단됨: key = {}", redisKey);
          return false;
        }
      }
    }
    return false;
  }
}
