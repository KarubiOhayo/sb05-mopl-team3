package io.mopl.socket.sse;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
