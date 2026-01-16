package io.mopl.api.notification.controller;

import io.mopl.api.common.dto.CursorResponse;
import io.mopl.api.notification.dto.NotificationDto;
import io.mopl.api.notification.dto.NotificationSearchRequest;
import io.mopl.api.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  public ResponseEntity<CursorResponse<NotificationDto>> list(
      @AuthenticationPrincipal(expression = "userId") UUID userId,
      @ModelAttribute @Valid NotificationSearchRequest request) {
    return ResponseEntity.ok(notificationService.findUnread(userId, request));
  }

  @DeleteMapping("/{notificationId}")
  public ResponseEntity<Void> read(
      @PathVariable UUID notificationId,
      @AuthenticationPrincipal(expression = "userId") UUID userId) {
    notificationService.delete(userId, notificationId);
    return ResponseEntity.noContent().build();
  }
}
