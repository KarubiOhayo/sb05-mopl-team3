package io.mopl.api.auth.service;

import io.mopl.api.auth.jwt.JwtTokenProvider;
import io.mopl.redis.constants.RedisKeyPrefix;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private final RedisTemplate<String, String> redisTemplate;
  private final JwtTokenProvider jwtTokenProvider;

  /** 리프레시 토큰 저장 */
  public void saveRefreshToken(UUID userId, String refreshToken) {
    String key = RedisKeyPrefix.REFRESH_TOKEN + userId.toString();
    long ttlSeconds = jwtTokenProvider.getRefreshTokenValidityInSeconds();
    redisTemplate.opsForValue().set(key, refreshToken, Duration.ofSeconds(ttlSeconds));
    log.debug("리프레시 토큰 저장: userId={}, key={}", userId, key);
  }

  /** 리프레시 토큰 조회 */
  public String getRefreshToken(UUID userId) {
    String key = RedisKeyPrefix.REFRESH_TOKEN + userId.toString();
    String refreshToken = redisTemplate.opsForValue().get(key);
    log.debug("리프레시 토큰 조회: userId={}, exists={}", userId, refreshToken != null);
    return refreshToken;
  }

  /** 리프레시 토큰 삭제 */
  public void deleteRefreshToken(UUID userId) {
    String key = RedisKeyPrefix.REFRESH_TOKEN + userId.toString();
    redisTemplate.delete(key);
    log.debug("리프레시 토큰 삭제: userId={}, key={}", userId, key);
  }

  /** 리프레시 토큰 유무 확인 */
  public boolean hasRefreshToken(UUID userId) {
    String key = RedisKeyPrefix.REFRESH_TOKEN + userId.toString();
    return Boolean.TRUE.equals(redisTemplate.hasKey(key));
  }
}
