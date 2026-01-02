package io.mopl.api.auth.service;

import io.mopl.api.auth.dto.AuthTokens;
import io.mopl.api.auth.dto.JwtDto;
import io.mopl.api.auth.dto.ResetPasswordRequest;
import io.mopl.api.auth.dto.SignInRequest;
import io.mopl.api.auth.jwt.JwtTokenProvider;
import io.mopl.api.common.error.AuthErrorCode;
import io.mopl.api.user.domain.AuthProvider;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.dto.UserDto;
import io.mopl.core.error.BusinessException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
  private final EmailService emailService;
  private final RedisTemplate<Object, Object> redisTemplate;

  private static final String RESET_LIMIT_KEY_PREFIX = "password-reset:limit:";
  private static final int MAX_RESET_ATTEMPTS = 3;
  private static final long RESET_LIMIT_DURATION = 300; // 5분

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
    if (!jwtTokenProvider.validateToken(refreshTokenFromCookie)
        || !jwtTokenProvider.isRefreshToken(refreshTokenFromCookie)) {
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

  /** 비밀번호 초기화 뒤 임시 비밀번호 발급 및 이메일 전송 */
  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    String email = request.getEmail();
    checkRateLimit(email);

    Optional<User> optionalUser = userRepository.findByEmail(email);
    if (optionalUser.isEmpty()) {
      return;
    }

    User user = optionalUser.get();
    if (user.getAuthProvider() != AuthProvider.LOCAL) {
      return;
    }

    String temporaryPassword = generateTemporaryPassword();
    user.setTempPasswordHash(passwordEncoder.encode(temporaryPassword));
    user.setTempPasswordExpiresAt(Instant.now().plus(3, ChronoUnit.MINUTES));

    emailService.sendTemporaryPassword(user.getEmail(), temporaryPassword);
  }

  /** Rate Limiting 체크 */
  private void checkRateLimit(String email) {
    String key = RESET_LIMIT_KEY_PREFIX + email;
    Integer attempts = (Integer) redisTemplate.opsForValue().get(key);

    if (attempts != null && attempts >= MAX_RESET_ATTEMPTS) {
      throw new BusinessException(AuthErrorCode.TOO_MANY_RESET_REQUESTS);
    }

    redisTemplate.opsForValue().increment(key);
    if (attempts == null) {
      redisTemplate.expire(key, RESET_LIMIT_DURATION, TimeUnit.SECONDS);
    }
  }

  /** 임시 비밀번호 랜덤 생성 */
  private String generateTemporaryPassword() {
    String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    int PASSWORD_LENGTH = 12;
    SecureRandom random = new SecureRandom();
    StringBuilder password = new StringBuilder(PASSWORD_LENGTH);

    for (int i = 0; i < PASSWORD_LENGTH; i++) {
      password.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
    }

    return password.toString();
  }
}
