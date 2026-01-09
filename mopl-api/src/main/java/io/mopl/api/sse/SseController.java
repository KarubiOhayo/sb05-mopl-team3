package io.mopl.api.sse;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// TODO 나중에 sse 개발 시작하면 패키지 통째로 삭제해주세요
@Tag(name = "SSE", description = "Server-Sent Events API (임시 구현)")
@RestController
@RequestMapping("/api/sse")
public class SseController {

  private static final Long DEFAULT_TIMEOUT = 60 * 60 * 1000L; // 1시간

  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(
      @RequestHeader(value = "Last-Event-ID", required = false) UUID lastEventId) {

    SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

    emitter.onTimeout(emitter::complete);
    emitter.onError(e -> emitter.complete());

    return emitter;
  }
}
