package io.mopl.api.auth.service;

import io.mopl.api.auth.dto.AuthTokens;
import io.mopl.api.auth.dto.JwtDto;
import io.mopl.api.auth.dto.SignInRequest;
import io.mopl.api.auth.jwt.JwtTokenProvider;
import io.mopl.api.common.error.AuthErrorCode;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.dto.UserDto;
import io.mopl.core.error.BusinessException;
import java.time.Instant;
import java.util.UUID;
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
  private final RefreshTokenService refreshTokenService;

  /** 로그인 */
  @Transactional
  public AuthTokens signIn(SignInRequest request) {
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

    String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
    refreshTokenService.saveRefreshToken(user.getId(), refreshToken);

    UserDto userDto = UserDto.from(user);

    JwtDto jwtDto = JwtDto.builder().userDto(userDto).accessToken(accessToken).build();

    return AuthTokens.builder().jwtDto(jwtDto).refreshToken(refreshToken).build();
  }

  /** 토큰 재발급 */
  @Transactional(readOnly = true)
  public AuthTokens reissueToken(String refreshTokenFromCookie) {
    if (!jwtTokenProvider.validateToken(refreshTokenFromCookie)) {
      log.warn("유효하지 않은 리프레시 토큰");
      throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    UUID userId = jwtTokenProvider.getUserId(refreshTokenFromCookie);

    String storeRefreshToken = refreshTokenService.getRefreshToken(userId);

    if (storeRefreshToken == null || !storeRefreshToken.equals(refreshTokenFromCookie)) {
      log.warn("리프레시 토큰이 일치하지 않음: userId={}", userId);
      throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

    if (user.isLocked()) {
      throw new BusinessException(AuthErrorCode.ACCOUNT_LOCKED);
    }

    String newAccessToken =
        jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());

    String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());
    refreshTokenService.saveRefreshToken(user.getId(), newRefreshToken);

    UserDto userDto = UserDto.from(user);

    JwtDto jwtDto = JwtDto.builder().userDto(userDto).accessToken(newAccessToken).build();

    log.info("토큰 재발급 성공: userId={}", userId);
    return AuthTokens.builder().jwtDto(jwtDto).refreshToken(newRefreshToken).build();
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
