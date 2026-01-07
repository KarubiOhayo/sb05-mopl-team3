package io.mopl.socket.watching.controller;

import io.mopl.socket.common.dto.CursorResponse;
import io.mopl.socket.watching.WatchingSessionService;
import io.mopl.socket.watching.dto.WatchingSessionDto;
import io.mopl.socket.watching.dto.WatchingSessionSearchRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class WatchingSessionController {
  private final WatchingSessionService watchingSessionService;

  @GetMapping("/contents/{contentId}/watching-sessions")
  public ResponseEntity<CursorResponse<WatchingSessionDto>> getWatchingSessions(
      @PathVariable UUID contentId, @ModelAttribute WatchingSessionSearchRequest request) {
    return null;
  }
}
