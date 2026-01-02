package io.mopl.api.user.service;

import io.mopl.api.common.error.UserErrorCode;
import io.mopl.api.user.domain.AuthProvider;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.domain.UserRole;
import io.mopl.api.user.dto.ChangePasswordRequest;
import io.mopl.api.user.dto.UserCreateRequest;
import io.mopl.api.user.dto.UserDto;
import io.mopl.api.user.dto.UserSummary;
import io.mopl.core.error.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

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

    String encodedPassword = passwordEncoder.encode(request.getPassword());
    user.setPasswordHash(encodedPassword);

    user.setTempPasswordHash(null);
    user.setTempPasswordExpiresAt(null);
  }
}
