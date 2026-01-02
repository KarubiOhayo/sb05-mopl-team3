package io.mopl.api.user.controller;

import io.mopl.api.user.dto.ChangePasswordRequest;
import io.mopl.api.user.dto.UserCreateRequest;
import io.mopl.api.user.dto.UserDto;
import io.mopl.api.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  /** 회원가입 */
  @PostMapping
  public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreateRequest request) {
    UserDto response = userService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /** 비밀번호 변경 */
  @PatchMapping("/{userId}/password")
  public ResponseEntity<Void> changePassword(
      @PathVariable UUID userId, @Valid @RequestBody ChangePasswordRequest request) {

    userService.changePassword(userId, request);
    return ResponseEntity.noContent().build();
  }
}
