package io.mopl.api.auth.service;

import io.mopl.api.auth.dto.AuthTokens;
import io.mopl.api.auth.dto.JwtDto;
import io.mopl.api.auth.dto.ResetPasswordRequest;
import io.mopl.api.auth.dto.SignInRequest;
import io.mopl.api.auth.event.PasswordResetEvent;
import io.mopl.api.auth.jwt.JwtTokenProvider;
import io.mopl.api.common.error.AuthErrorCode;
import io.mopl.api.user.domain.AuthProvider;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.dto.UserDto;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import io.mopl.redis.constants.RedisKeyPrefix;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
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
  private final StringRedisTemplate stringRedisTemplate;
  private final ApplicationEventPublisher eventPublisher;

  private static final int MAX_RESET_ATTEMPTS = 3;
  private static final long RESET_LIMIT_DURATION = 300; // 5분
  private static final long TEMP_PASSWORD_EXPIRATION = 180; // 3분

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

    validatePassword(user, request.getPassword());

    String accessToken =
        jwtTokenProvider.createAccessToken(
            user.getId(),
            user.getEmail(),
            user.getRole().name(),
            user.getName(),
            user.getProfileImageUrl());

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
      log.warn("리프레시 토큰이 일치하지 않음");
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
        jwtTokenProvider.createAccessToken(
            user.getId(),
            user.getEmail(),
            user.getRole().name(),
            user.getName(),
            user.getProfileImageUrl());

    String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());
    refreshTokenService.saveRefreshToken(user.getId(), newRefreshToken);

    UserDto userDto = UserDto.from(user);

    JwtDto jwtDto = JwtDto.builder().userDto(userDto).accessToken(newAccessToken).build();

    return AuthTokens.builder().jwtDto(jwtDto).refreshToken(newRefreshToken).build();
  }

  /** 비밀번호 검증 */
  private void validatePassword(User user, String rawPassword) {
    boolean isPasswordValid;

    String tempPasswordKey = RedisKeyPrefix.TEMP_PASSWORD + user.getId();
    String tempPasswordHash = stringRedisTemplate.opsForValue().get(tempPasswordKey);

    if (tempPasswordHash != null) {
      isPasswordValid = passwordEncoder.matches(rawPassword, tempPasswordHash);
      if (isPasswordValid) {
        return;
      }
    }

    isPasswordValid = passwordEncoder.matches(rawPassword, user.getPasswordHash());

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
    String encodedPassword = passwordEncoder.encode(temporaryPassword);

    String tempPasswordKey = RedisKeyPrefix.TEMP_PASSWORD + user.getId();
    stringRedisTemplate
        .opsForValue()
        .set(tempPasswordKey, encodedPassword, TEMP_PASSWORD_EXPIRATION, TimeUnit.SECONDS);

    PasswordResetEvent event =
        new PasswordResetEvent(user.getId(), user.getEmail(), temporaryPassword);
    eventPublisher.publishEvent(event);
  }

  /** Rate Limiting 체크 */
  private void checkRateLimit(String email) {
    String hashedEmail = hashEmail(email);
    String key = RedisKeyPrefix.PASSWORD_RESET_LIMIT + hashedEmail;

    Long attempts = stringRedisTemplate.opsForValue().increment(key);

    if (attempts == 1) {
      stringRedisTemplate.expire(key, RESET_LIMIT_DURATION, TimeUnit.SECONDS);
    }

    if (attempts > MAX_RESET_ATTEMPTS) {
      throw new BusinessException(AuthErrorCode.TOO_MANY_RESET_REQUESTS);
    }
  }

  /** 이메일을 SHA-256으로 해싱 */
  private String hashEmail(String email) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(email.toLowerCase().getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      log.error("SHA-256 알고리즘을 사용할 수 없습니다", e);
      throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
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
