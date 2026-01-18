package io.mopl.api.user.controller;

import io.mopl.api.common.config.AuthUser;
import io.mopl.api.common.error.AuthErrorCode;
import io.mopl.api.user.domain.AuthProvider;
import io.mopl.api.user.dto.LinkedProviderDto;
import io.mopl.api.user.service.UserLinkedProviderService;
import io.mopl.core.error.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserLinkedProviderController {

  private final UserLinkedProviderService linkedProviderService;

  @GetMapping("/api/users/me/linked-providers")
  public ResponseEntity<List<LinkedProviderDto>> getLinkedProviders(
      @AuthenticationPrincipal AuthUser authUser) {
    List<LinkedProviderDto> providers =
        linkedProviderService.getLinkedProviders(authUser.getUserId());
    return ResponseEntity.ok(providers);
  }

  @DeleteMapping("/api/users/me/linked-providers/{provider}")
  public ResponseEntity<Void> unlinkProvider(
      @PathVariable String provider, @AuthenticationPrincipal AuthUser authUser) {
    AuthProvider authProvider = parseProvider(provider);
    linkedProviderService.unlinkProvider(authUser.getUserId(), authProvider);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/api/users/me/link-provider/{provider}")
  public ResponseEntity<Void> unlinkProviderAlt(
      @PathVariable String provider, @AuthenticationPrincipal AuthUser authUser) {
    AuthProvider authProvider = parseProvider(provider);
    linkedProviderService.unlinkProvider(authUser.getUserId(), authProvider);
    return ResponseEntity.noContent().build();
  }

  private AuthProvider parseProvider(String provider) {
    try {
      return AuthProvider.valueOf(provider.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BusinessException(AuthErrorCode.UNSUPPORTED_PROVIDER);
    }
  }
}
