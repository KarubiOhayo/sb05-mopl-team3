package io.mopl.socket.sse;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class SseService {
  private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

  public void add(String userId, SseEmitter emitter) {
    emitters.put(userId, emitter);

    log.info("SSE emitter 추가됨: userId={}", userId);

    emitter.onCompletion(
        () -> {
          log.info("SSE emitter 완료됨: userId={}", userId);
          emitters.remove(userId);
        });
    emitter.onTimeout(
        () -> {
          log.info("SSE emitter 타임아웃: userId={}", userId);
          emitters.remove(userId);
        });
    emitter.onError(
        (e) -> {
          log.error("SSE emitter 에러 발생: userId={}", userId, e);
          emitters.remove(userId);
        });
  }

  public void send(String userId, String name, Object data) {
    SseEmitter emitter = emitters.get(userId);
    if (emitter != null) {
      try {
        emitter.send(SseEmitter.event().id(UUID.randomUUID().toString()).name(name).data(data));
      } catch (IOException e) {
        log.error("SSE 전송 실패: userId={}", userId, e);
        emitters.remove(userId);
      }
    }
  }
}
