package io.mopl.api.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TemporaryPasswordService {

  private static final String TEMP_PASSWORD_KEY_PREFIX = "temp_password_";
  private static final Duration TEMP_PASSWORD_EXPIRATION = Duration.ofMinutes(3);
  private static final String CHARACTERS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
  private static final int PASSWORD_LENGTH = 12;

  private final StringRedisTemplate redisTemplate;

  /** 임시 비밀번호 생성 후 Redis에 저장 */
  public String createAndSaveTemporaryPassword(UUID userId) {
    String temporaryPassword = generateTemporaryPassword();
    String key = TEMP_PASSWORD_KEY_PREFIX + userId.toString();

    redisTemplate.opsForValue().set(key, temporaryPassword, TEMP_PASSWORD_EXPIRATION);

    return temporaryPassword;
  }

  /** 임시 비밀번호 생성 */
  private String generateTemporaryPassword() {
    SecureRandom random = new SecureRandom();
    StringBuilder password = new StringBuilder(PASSWORD_LENGTH);

    for (int i = 0; i < PASSWORD_LENGTH; i++) {
      password.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
    }
    return password.toString();
  }

  /** 임시 비밀번호 조회 */
  public Optional<String> getTemporaryPassword(UUID userId) {
    String Key = TEMP_PASSWORD_KEY_PREFIX + userId.toString();
    String temporaryPassword = redisTemplate.opsForValue().get(Key);

    return Optional.ofNullable(temporaryPassword);
  }

  /** 임시 비밀번호 삭제 */
  public void deleteTemporaryPassword(UUID userId) {
    String key = TEMP_PASSWORD_KEY_PREFIX + userId.toString();
    redisTemplate.delete(key);
  }
}
