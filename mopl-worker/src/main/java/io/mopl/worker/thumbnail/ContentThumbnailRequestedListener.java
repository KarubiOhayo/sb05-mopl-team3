package io.mopl.worker.thumbnail;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.mopl.core.event.thumbnail.ContentThumbnailRequestedEvent;
import io.mopl.core.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/** 썸네일 요청 Kafka 이벤트를 수신해 비동기 처리로 위임한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentThumbnailRequestedListener {

  private final ContentThumbnailRequestedHandler handler;
  private final MeterRegistry meterRegistry;

  /**
   * 썸네일 요청 이벤트를 수신한다.
   *
   * @param event 요청 이벤트
   * @param acknowledgment Kafka ACK
   */
  @KafkaListener(
      topics = KafkaTopics.CONTENT_THUMBNAIL_REQUESTED,
      containerFactory = "manualAckKafkaListenerContainerFactory")
  public void handle(ContentThumbnailRequestedEvent event, Acknowledgment acknowledgment) {
    String runTag = RunIdResolver.resolveFromKey(event.s3Key());
    Counter.builder("worker.thumbnail.listener.received")
        .tags("run_id", runTag)
        .register(meterRegistry)
        .increment();
    log.debug(
        "썸네일 요청 이벤트 수신: eventId={}, contentId={}, s3Key={}, attempt={}",
        event.eventId(),
        event.contentId(),
        event.s3Key(),
        event.attempt());
    try {
      handler.handleAsync(event, acknowledgment);
    } catch (TaskRejectedException ex) {
      Counter.builder("worker.thumbnail.listener.rejected")
          .tags("run_id", runTag, "reason", "TaskRejected")
          .register(meterRegistry)
          .increment();
      log.warn(
          "썸네일 요청 처리 거부: eventId={}, contentId={}, s3Key={}",
          event.eventId(),
          event.contentId(),
          event.s3Key(),
          ex);
      throw ex;
    }
  }
}
