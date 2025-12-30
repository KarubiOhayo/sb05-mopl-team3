package io.mopl.api.user.service;

import io.mopl.api.common.error.AuthErrorCode;
import io.mopl.api.common.error.UserErrorCode;
import io.mopl.api.user.domain.AuthProvider;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.domain.UserRole;
import io.mopl.api.user.dto.UserCreateRequest;
import io.mopl.api.user.dto.UserDto;
import io.mopl.api.user.dto.UserSummary;
import io.mopl.core.error.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    return UserDto.builder()
        .id(savedUser.getId())
        .createdAt(savedUser.getCreatedAt())
        .email(savedUser.getEmail())
        .name(savedUser.getName())
        .profileImageUrl(savedUser.getProfileImageUrl())
        .role(savedUser.getRole())
        .locked(savedUser.isLocked())
        .build();
  }

  /** 사용자 확인 */
  @Transactional(readOnly = true)
  public UserSummary getUserSummary(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

    return UserSummary.builder()
        .userId(user.getId())
        .name(user.getName())
        .profileImageUrl(user.getProfileImageUrl())
        .build();
  }
}
