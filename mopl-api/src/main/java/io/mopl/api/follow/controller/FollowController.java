package io.mopl.api.follow.controller;

import io.mopl.api.follow.dto.FollowDto;
import io.mopl.api.follow.dto.FollowRequest;
import io.mopl.api.follow.service.FollowService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/follows")
public class FollowController {

  private final FollowService followService;

  @PostMapping
  public ResponseEntity<FollowDto> create(
      @Valid @RequestBody FollowRequest request,
      @AuthenticationPrincipal(expression = "userId") UUID userId) {
    FollowDto dto = followService.create(request, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
  }

  @GetMapping("/followed-by-me")
  public ResponseEntity<Boolean> followedByMe(
      @RequestParam UUID followeeId, @AuthenticationPrincipal(expression = "userId") UUID userId) {
    boolean result = followService.followedByMe(followeeId, userId);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/count")
  public ResponseEntity<Long> count(
      @RequestParam UUID followeeId, @AuthenticationPrincipal(expression = "userId") UUID userId) {
    long result = followService.count(followeeId, userId);
    return ResponseEntity.ok(result);
  }

  @DeleteMapping("/{followId}")
  public ResponseEntity<Void> cancelFollow(
      @PathVariable UUID followId, @AuthenticationPrincipal(expression = "userId") UUID userId) {
    followService.cancel(followId, userId);
    return ResponseEntity.noContent().build();
  }
}
