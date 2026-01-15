package io.mopl.socket.auth.jwt;

import io.mopl.socket.websocket.security.SocketUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String token = resolveToken(request);
    String requestUri = request.getRequestURI();

    // SSE 요청인 경우에만 상세 로그 출력 (노이즈 방지)
    if (requestUri.startsWith("/api/sse")) {
      log.info("SSE 인증 필터 - URI: {}, 토큰 존재 여부: {}", requestUri, StringUtils.hasText(token));
    }

    if (StringUtils.hasText(token)) {
      if (jwtTokenProvider.validateToken(token)) {
        Authentication authentication = getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        if (requestUri.startsWith("/api/sse")) {
          log.info("SSE 인증 필터 - 인증 성공: 사용자={}", authentication.getName());
        }
      } else {
        if (requestUri.startsWith("/api/sse")) {
          log.warn("SSE 인증 필터 - 유효하지 않은 토큰");
        }
      }
    } else {
      if (requestUri.startsWith("/api/sse")) {
        log.warn("SSE 인증 필터 - 토큰 미제공");
      }
    }

    filterChain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    // SSE의 경우 쿼리 파라미터로 토큰을 전달하는 경우도 고려 (선택 사항)
    String queryToken = request.getParameter("token");
    if (StringUtils.hasText(queryToken)) {
      return queryToken;
    }
    return null;
  }

  private Authentication getAuthentication(String token) {
    UUID userId = jwtTokenProvider.getUserId(token);
    String role = jwtTokenProvider.getRole(token);
    String name = jwtTokenProvider.getName(token);
    String profileImageUrl = jwtTokenProvider.getProfileImageUrl(token);

    SocketUserPrincipal principal =
        new SocketUserPrincipal(
            userId,
            null, // email (필요시 추가)
            role,
            name,
            profileImageUrl);

    return new UsernamePasswordAuthenticationToken(
        principal, "", Collections.singletonList(new SimpleGrantedAuthority(role)));
  }
}
