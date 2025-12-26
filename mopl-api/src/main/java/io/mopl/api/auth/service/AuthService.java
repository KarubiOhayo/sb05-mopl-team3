package io.mopl.api.auth.service;

import io.mopl.api.auth.dto.JwtDto;
import io.mopl.api.auth.dto.SignInRequest;
import io.mopl.api.auth.jwt.JwtTokenProvider;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.dto.UserDto;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  /**
   * 로그인
   */
  @Transactional
  public JwtDto signIn(SignInRequest request) {
    User user = userRepository.findByEmail(request.getUsername())
        .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + request.getUsername()));

    if (user.getLocked()) {
      throw new BadCredentialsException("잠긴 계정입니다");
    }

    validatePassword(request.getPassword(), user);

    String accessToken = jwtTokenProvider.createAccessToken(
        user.getId(),
        user.getEmail(),
        user.getRole().name()
    );

    UserDto userDto = UserDto.builder()
        .id(user.getId())
        .createdAt(user.getCreatedAt())
        .email(user.getEmail())
        .name(user.getName())
        .profileImageUrl(user.getProfileImageUrl())
        .role(user.getRole())
        .locked(user.getLocked())
        .build();

    return JwtDto.builder()
        .userDto(userDto)
        .accessToken(accessToken)
        .build();
  }

  /**
   * 비밀번호 검증
   */
  private void validatePassword(String rawPassword, User user) {
    boolean isPasswordValid = passwordEncoder.matches(rawPassword, user.getPasswordHash());

    // 비밀번호 틀렸을 시 임시 비밀번호 체크
    if (!isPasswordValid && user.getTempPasswordHash() != null) {
      if (user.getTempPasswordExpiresAt() != null &&
          user.getTempPasswordExpiresAt().isAfter(LocalDateTime.now())) {
        isPasswordValid = passwordEncoder.matches(rawPassword, user.getTempPasswordHash());
      }
    }

    if (!isPasswordValid) {
      throw new BadCredentialsException("비밀번호가 일치하지 않습니다");
    }
  }
}