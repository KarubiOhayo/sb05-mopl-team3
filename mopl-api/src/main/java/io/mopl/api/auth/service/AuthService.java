package io.mopl.api.auth.service;

import io.mopl.api.auth.dto.JwtDto;
import io.mopl.api.auth.dto.SignInRequest;
import io.mopl.api.auth.jwt.JwtTokenProvider;
import io.mopl.api.common.error.AuthErrorCode;
import io.mopl.api.common.error.BusinessException;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.dto.UserDto;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  /** 로그인 */
  @Transactional
  public JwtDto signIn(SignInRequest request) {
    User user =
        userRepository
            .findByEmail(request.getUsername())
            .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

    if (user.isLocked()) {
      throw new BusinessException(AuthErrorCode.ACCOUNT_LOCKED);
    }

    validatePassword(request.getPassword(), user);

    String accessToken =
        jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());

    UserDto userDto =
        UserDto.builder()
            .id(user.getId())
            .createdAt(user.getCreatedAt())
            .email(user.getEmail())
            .name(user.getName())
            .profileImageUrl(user.getProfileImageUrl())
            .role(user.getRole())
            .locked(user.isLocked())
            .build();

    return JwtDto.builder().userDto(userDto).accessToken(accessToken).build();
  }

  /** 비밀번호 검증 */
  private void validatePassword(String rawPassword, User user) {
    boolean isPasswordValid = passwordEncoder.matches(rawPassword, user.getPasswordHash());

    // 비밀번호 틀렸을 시 임시 비밀번호 체크
    if (!isPasswordValid && user.getTempPasswordHash() != null) {
      if (user.getTempPasswordExpiresAt() != null
          && user.getTempPasswordExpiresAt().isAfter(Instant.now())) {
        isPasswordValid = passwordEncoder.matches(rawPassword, user.getTempPasswordHash());
      }
    }

    if (!isPasswordValid) {
      throw new BusinessException(AuthErrorCode.INVALID_PASSWORD);
    }
  }
}
