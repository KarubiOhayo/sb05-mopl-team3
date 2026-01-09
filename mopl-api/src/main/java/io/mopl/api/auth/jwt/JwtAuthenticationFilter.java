package io.mopl.api.auth.jwt;

import io.mopl.api.common.config.AuthUser;
import io.mopl.api.common.error.AuthErrorCode;
import io.mopl.api.common.error.UserErrorCode;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.core.error.BusinessException;
import io.mopl.redis.constants.RedisKeyPrefix;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final RedisTemplate<String, Boolean> redisTemplate;
  private final UserRepository userRepository;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    try {
      String token = getTokenFromRequest(request);

      if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
        UUID userId = jwtTokenProvider.getUserId(token);
        String email = jwtTokenProvider.getEmail(token);
        String role = jwtTokenProvider.getRole(token);

        AuthUser authUser = AuthUser.builder().userId(userId).email(email).role(role).build();

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                authUser, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("JWT 인증 성공: userId={}, email={}, role={}", userId, email, role);
      }
    } catch (Exception e) {
      log.error("JWT 인증 실패: {}", e.getMessage());
    }

    filterChain.doFilter(request, response);
  }

  /** Request Header에서 Bearer 토큰 추출 */
  private String getTokenFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");

    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }

    return null;
  }

  /** Redis에서 계정 잠금 상태 확인 */
  private void checkUserLocked(UUID userId) {
    String redisKey = RedisKeyPrefix.USER_LOCKED + userId;
    Boolean isLocked = redisTemplate.opsForValue().get(redisKey);

    if (isLocked == null) {
      User user =
          userRepository
              .findById(userId)
              .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

      isLocked = user.isLocked();

      redisTemplate
          .opsForValue()
          .set(
              redisKey,
              isLocked,
              Duration.ofSeconds(jwtTokenProvider.getAccessTokenValidityInSeconds()));
    }

    if (Boolean.TRUE.equals(isLocked)) {
      throw new BusinessException(AuthErrorCode.ACCOUNT_LOCKED);
    }
  }
}
