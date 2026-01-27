package io.mopl.worker.notification;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationMetrics {

  private final MeterRegistry meterRegistry;

  public void recordFailure(String type, String reason) {
    Counter.builder("worker.notification.handle.failures")
        .tags("type", type, "reason", reason)
        .register(meterRegistry)
        .increment();
  }
}
