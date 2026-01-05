package io.mopl.api.auth.controller;

import io.mopl.api.auth.dto.AuthTokens;
import io.mopl.api.auth.dto.SignInRequest;
import io.mopl.api.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
  @PostMapping("/sign-in")
  public ResponseEntity<AuthTokens> signIn(@Valid @RequestBody SignInRequest request) {
    AuthTokens tokens = authService.signIn(request);
    return ResponseEntity.ok(tokens);
  }

  @Operation(summary = "토큰 갱신", description = "Refresh Token으로 Access Token을 갱신합니다.")
  @PostMapping("/refresh")
  public ResponseEntity<AuthTokens> refresh(@RequestBody String refreshToken) {
    AuthTokens tokens = authService.reissueToken(refreshToken);
    return ResponseEntity.ok(tokens);
  }
}
