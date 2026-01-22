package io.mopl.socket.sse;

import io.mopl.core.error.BusinessException;
import io.mopl.socket.common.error.SocketErrorCode;
import io.mopl.socket.websocket.security.SocketUserPrincipal;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

  private static final Long DEFAULT_TIMEOUT = 60 * 60 * 1000L; // 1시간

  private final SseService sseService;

  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(
      @RequestHeader(value = "Last-Event-ID", required = false) UUID lastEventId,
      Principal principal) {

    SocketUserPrincipal user = resolvePrincipal(principal);
    SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

    // 연결 직후 더미 데이터 전송 (연결 확인용)
    try {
      emitter.send(SseEmitter.event().name("connect").data("connected"));
    } catch (Exception e) {
      log.warn("SSE 연결 이벤트 전송 실패: {}", e.getMessage());
      return emitter;
    }

    sseService.add(user.userId().toString(), emitter);

    return emitter;
  }

  private SocketUserPrincipal resolvePrincipal(Principal principal) {
    if (principal instanceof UsernamePasswordAuthenticationToken auth
        && auth.getPrincipal() instanceof SocketUserPrincipal socketUser) {
      return socketUser;
    }
    if (principal instanceof SocketUserPrincipal socketUser) {
      return socketUser;
    }
    throw new BusinessException(SocketErrorCode.MISSING_AUTHENTICATION);
  }
}
