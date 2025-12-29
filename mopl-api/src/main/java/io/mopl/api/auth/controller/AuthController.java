package io.mopl.api.auth.controller;

import io.mopl.api.auth.dto.JwtDto;
import io.mopl.api.auth.dto.SignInRequest;
import io.mopl.api.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  /** 로그인 Content-Type: application/x-www-form-urlencoded */
  @PostMapping(value = "/sign-in", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<JwtDto> signIn(@Valid @ModelAttribute SignInRequest request) {
    log.info("로그인 요청: {}", request.getUsername());
    JwtDto response = authService.signIn(request);
    return ResponseEntity.ok(response);
  }
}
