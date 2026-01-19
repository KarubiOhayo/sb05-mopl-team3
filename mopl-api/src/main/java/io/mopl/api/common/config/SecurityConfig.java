package io.mopl.api.common.config;

import io.mopl.api.auth.jwt.JwtAuthenticationFilter;
import io.mopl.api.auth.oauth2.CustomOAuth2AuthorizationRequestResolver;
import io.mopl.api.auth.oauth2.OAuth2AuthenticationFailureHandler;
import io.mopl.api.auth.oauth2.OAuth2AuthenticationSuccessHandler;
import io.mopl.api.auth.service.CustomOAuth2UserService;
import java.util.Arrays;
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
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
  private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
  private final CustomOAuth2AuthorizationRequestResolver customOAuth2AuthorizationRequestResolver;

  // 개발 중 테스트를 위한 csrf 비활성화 메서드
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
  public SecurityFilterChain filterChain(
      HttpSecurity http, CookieCsrfTokenRepository csrfTokenRepository) throws Exception {

    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
    requestHandler.setCsrfRequestAttributeName("_csrf");

    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(csrfTokenRepository)
                    .csrfTokenRequestHandler(requestHandler)
                    .ignoringRequestMatchers(
                        request -> {
                          String method = request.getMethod();
                          String path = request.getRequestURI();
                          return (method.equals("POST") && path.equals("/api/auth/sign-in"))
                              || (method.equals("POST") && path.equals("/api/users"))
                              || (method.equals("POST") && path.equals("/api/auth/reset-password"))
                              || path.startsWith("/login/oauth2/");
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

                    /* ========== Swagger ========== */
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**")
                    .permitAll()
                    .requestMatchers("/actuator/health", "/actuator/prometheus")
                    .permitAll()

                    /* ========== SPA 프론트엔드 라우트 ========== */
                    .requestMatchers(
                        "/profiles/**",
                        "/playlists/**",
                        "/contents/**",
                        "/conversations/**",
                        "/notifications/**")
                    .permitAll()

                    /* ========== CSRF 토큰 발급 ========== */
                    .requestMatchers(HttpMethod.GET, "/api/auth/csrf-token")
                    .permitAll()

                    /* ========== 인증 관리 ========== */
                    // 전체: 모든 기능
                    .requestMatchers(HttpMethod.GET, "/api/auth/refresh")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/sign-in", "/api/auth/refresh")
                    .permitAll()
                    .requestMatchers("/api/auth/**")
                    .permitAll()

                    /* ========== OAuth2 인증 ==========*/
                    .requestMatchers("/oauth2/**", "/login/oauth2/**")
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
                    // 유저: 시청 세션 조회
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/users/{watcherId}/watching-sessions",
                        "/api/contents/{contentId}/watching-sessions")
                    .authenticated()

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
        /* OAuth 로그인 설정 */
        .oauth2Login(
            oauth2 ->
                oauth2
                    .authorizationEndpoint(
                        authorization ->
                            authorization
                                .baseUri("/oauth2/authorization")
                                .authorizationRequestResolver(
                                    customOAuth2AuthorizationRequestResolver))
                    .redirectionEndpoint(redirection -> redirection.baseUri("/login/oauth2/code/*"))
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    .failureHandler(oAuth2AuthenticationFailureHandler))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // 허용할 Origin (프론트엔드 URL)
    configuration.setAllowedOrigins(
        Arrays.asList("http://localhost:8085", "http://192.168.219.105:8085"));

    // 허용할 HTTP 메서드
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

    // 허용할 헤더
    configuration.setAllowedHeaders(Arrays.asList("*"));

    // 인증 정보(쿠키) 포함 허용
    configuration.setAllowCredentials(true);

    // Preflight 요청 캐시 시간 (1시간)
    configuration.setMaxAge(3600L);

    // 응답 헤더 노출 (프론트엔드에서 읽을 수 있는 헤더)
    configuration.setExposedHeaders(Arrays.asList("Authorization", "Set-Cookie", "X-XSRF-TOKEN"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }
}
