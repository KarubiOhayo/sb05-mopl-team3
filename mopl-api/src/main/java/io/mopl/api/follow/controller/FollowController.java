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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
