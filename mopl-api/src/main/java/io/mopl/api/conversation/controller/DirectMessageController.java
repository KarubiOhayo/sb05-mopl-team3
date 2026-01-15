package io.mopl.api.conversation.controller;

import io.mopl.api.common.dto.CursorResponse;
import io.mopl.api.common.error.UserErrorCode;
import io.mopl.api.conversation.dto.DirectMessageDto;
import io.mopl.api.conversation.dto.DirectMessageSearchRequest;
import io.mopl.api.conversation.service.DirectMessageService;
import io.mopl.core.error.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations/{conversationId}/direct-messages")
public class DirectMessageController {
  private final DirectMessageService directMessageService;

  @GetMapping
  public ResponseEntity<CursorResponse<DirectMessageDto>> list(
      @AuthenticationPrincipal(expression = "userId") UUID userId,
      @PathVariable UUID conversationId,
      @ModelAttribute DirectMessageSearchRequest request) {
    return ResponseEntity.ok(directMessageService.find(userId, conversationId, request));
  }

  @PostMapping("/{directMessageId}/read")
  public ResponseEntity<Void> read(
      @PathVariable UUID conversationId,
      @PathVariable UUID directMessageId,
      @AuthenticationPrincipal(expression = "userId") UUID userId) {
    if (userId == null) {
      log.warn("DM | 읽음 처리 | 실패: 인증 사용자 ID 없음");
      throw new BusinessException(UserErrorCode.UNAUTHORIZED);
    }
    directMessageService.read(conversationId, directMessageId, userId);
    return ResponseEntity.noContent().build();
  }
}
