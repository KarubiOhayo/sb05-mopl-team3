package io.mopl.api.user.controller;

import io.mopl.api.common.config.AuthUser;
import io.mopl.api.user.dto.ChangePasswordRequest;
import io.mopl.api.user.dto.UserCreateRequest;
import io.mopl.api.user.dto.UserDto;
import io.mopl.api.user.dto.UserUpdateRequest;
import io.mopl.api.user.service.UserService;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

  @GetMapping("/{userId}")
  public ResponseEntity<UserDto> getUserDetail(@PathVariable("userId") UUID userId) {

    UserDto response = userService.getUserDetails(userId);
    return ResponseEntity.ok(response);
  }

  /** 비밀번호 변경 */
  @PatchMapping("/{userId}/password")
  public ResponseEntity<Void> changePassword(
      @PathVariable UUID userId,
      @Valid @RequestBody ChangePasswordRequest request,
      @AuthenticationPrincipal AuthUser authUser) {

    if (!userId.equals(authUser.getUserId())) {
      throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    userService.changePassword(userId, request);
    return ResponseEntity.noContent().build();
  }

  /** 프로필 변경 */
  @PatchMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<UserDto> updateProfile(
      @PathVariable UUID userId,
      @RequestPart("request") @Valid UserUpdateRequest request,
      @RequestPart(value = "image", required = false) MultipartFile profileImage,
      @AuthenticationPrincipal AuthUser authUser) {

    if (!userId.equals(authUser.getUserId())) {
      throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    UserDto response = userService.updateProfile(userId, request, profileImage);
    return ResponseEntity.ok(response);
  }
}
