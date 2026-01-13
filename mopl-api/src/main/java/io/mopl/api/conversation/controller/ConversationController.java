package io.mopl.api.conversation.controller;

import io.mopl.api.conversation.dto.ConversationCreateRequest;
import io.mopl.api.conversation.dto.ConversationDto;
import io.mopl.api.conversation.service.ConversationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {
  private final ConversationService conversationService;

  @PostMapping
  public ResponseEntity<ConversationDto> create(
      @AuthenticationPrincipal(expression = "userId") UUID userId,
      @Valid @RequestBody ConversationCreateRequest request) {

    log.info("대화 생성 요청 수신: userId={}, withUserId={}", userId, request.withUserId());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(conversationService.create(userId, request.withUserId()));
  }

  @GetMapping("/{conversationId}")
  public ResponseEntity<ConversationDto> findById(
      @PathVariable UUID conversationId,
      @AuthenticationPrincipal(expression = "userId") UUID userId) {
    return ResponseEntity.ok(conversationService.findById(conversationId, userId));
  }

  @GetMapping("/with")
  public ResponseEntity<ConversationDto> findByWithUserId(
      @AuthenticationPrincipal(expression = "userId") UUID userId,
      @RequestParam("userId") UUID withUserId) {
    return ResponseEntity.ok(conversationService.findByWithUserId(userId, withUserId));
  }
}
