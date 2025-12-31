package io.mopl.api.common.config;

import io.mopl.api.auth.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  // 개발 중 테스트를 위한 csrf 임시 비활성화 메서드
  //  @Bean
  //  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
  //    http
  //        .csrf(csrf -> csrf.disable())
  //        .sessionManagement(session -> session
  //            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
  //        .authorizeHttpRequests(auth -> auth
  //            .requestMatchers("/api/users/register", "/api/users/login").permitAll()
  //            .requestMatchers("/api/users/**").authenticated()
  //            .anyRequest().permitAll())
  //        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
  //
  //    return http.build();
  //  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringRequestMatchers(
                        request -> {
                          String method = request.getMethod();
                          String path = request.getRequestURI();
                          return (method.equals("POST") && path.equals("/api/auth/sign-in"))
                              || (method.equals("POST") && path.equals("/api/users"));
                        }))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth
                    /* ========== 정적 리소스 ========== */
                    .requestMatchers("/", "/index.html")
                    .permitAll()
                    .requestMatchers(
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/static/**",
                        "/assets/**",
                        "/webjars/**",
                        "/vite.svg")
                    .permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**")
                    .permitAll()

                    /* ========== 인증 관리 ========== */
                    // 전체: 모든 기능
                    .requestMatchers("/api/auth/**")
                    .permitAll()

                    /* ========== 사용자 관리 ========== */
                    // 관리자: 목록 조회, 권한 수정, 계정 잠금
                    .requestMatchers(HttpMethod.GET, "/api/users")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        HttpMethod.PATCH, "/api/users/{userId}/role", "/api/users/{userId}/locked")
                    .hasRole("ADMIN")
                    // 유저: 프로필 변경, 비밀번호 변경
                    .requestMatchers(
                        HttpMethod.PATCH, "/api/users/{userId}", "/api/users/{userId}/password")
                    .authenticated()
                    // 전체: 회원가입, 상세 조회
                    .requestMatchers(HttpMethod.POST, "/api/users")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/users/{userId}")
                    .permitAll()

                    /* ========== 리뷰 관리 ========== */
                    // 유저: 생성, 수정, 삭제
                    .requestMatchers(HttpMethod.POST, "/api/reviews")
                    .authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/reviews/{reviewId}")
                    .authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/reviews/{reviewId}")
                    .authenticated()
                    // 전체: 목록 조회
                    .requestMatchers(HttpMethod.GET, "/api/reviews")
                    .permitAll()

                    /* ========== 플레이리스트 관리 ========== */
                    // 유저: 생성, 수정, 삭제, 구독, 콘텐츠 추가/삭제, 단건 조회
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/playlists",
                        "/api/playlists/{playlistId}/subscription",
                        "/api/playlists/{playlistId}/contents/{contentId}")
                    .authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/playlists/{playlistId}")
                    .authenticated()
                    .requestMatchers(
                        HttpMethod.DELETE,
                        "/api/playlists/{playlistId}",
                        "/api/playlists/{playlistId}/subscription",
                        "/api/playlists/{playlistId}/contents/{contentId}")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/playlists/{playlistId}")
                    .authenticated()
                    // 전체: 목록 조회
                    .requestMatchers(HttpMethod.GET, "/api/playlists")
                    .permitAll()

                    /* ========== 팔로우 관리 ========== */
                    // 유저: 팔로우, 팔로우 취소
                    .requestMatchers(HttpMethod.POST, "/api/follows")
                    .authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/follows/{followId}")
                    .authenticated()
                    // 전체: 팔로우 여부 조회, 팔로워 수 조회
                    .requestMatchers(
                        HttpMethod.GET, "/api/follows/followed-by-me", "/api/follows/count")
                    .permitAll()

                    /* ========== 콘텐츠 관리 ========== */
                    // 관리자: 생성, 수정, 삭제
                    .requestMatchers(HttpMethod.POST, "/api/contents")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/contents/{contentId}")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/contents/{contentId}")
                    .hasRole("ADMIN")
                    // 전체: 목록 조회, 단건 조회
                    .requestMatchers(HttpMethod.GET, "/api/contents", "/api/contents/{contentId}")
                    .permitAll()

                    /* ========== 다이렉트 메시지 ========== */
                    // 유저: 전체 기능
                    .requestMatchers("/api/conversations/**")
                    .authenticated()

                    /* ========== 시청 세션 관리 ========== */
                    // 관리자: 전체 기능
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/users/{watcherId}/watching-sessions",
                        "/api/contents/{contentId}/watching-sessions")
                    .hasRole("ADMIN")

                    /* ========== SSE ========== */
                    // 유저: 전체 기능
                    .requestMatchers(HttpMethod.GET, "/api/sse")
                    .authenticated()

                    /* ========== 알림 ========== */
                    // 유저: 전체 기능
                    .requestMatchers("/api/notifications/**")
                    .authenticated()

                    // 나머지는 인증 필요
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
