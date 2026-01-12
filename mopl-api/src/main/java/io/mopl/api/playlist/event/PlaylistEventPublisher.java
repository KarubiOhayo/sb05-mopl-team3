package io.mopl.api.playlist.event;

import io.mopl.core.event.playlist.PlaylistContentAddedEvent;
import io.mopl.core.event.playlist.PlaylistCreatedEvent;
import io.mopl.core.event.playlist.PlaylistSubscribedEvent;
import io.mopl.core.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaylistEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void publishCreated(PlaylistCreatedEvent event) {
    kafkaTemplate
        .send(KafkaTopics.PLAYLIST_CREATED, event.playlistId(), event)
        .whenComplete(
            (result, ex) -> {
              if (ex != null) {
                log.error("플레이리스트 생성 이벤트 발행 실패 eventId={}", event.eventId(), ex);
              }
            });
  }

  public void publishSubscribed(PlaylistSubscribedEvent event) {
    kafkaTemplate
        .send(KafkaTopics.PLAYLIST_SUBSCRIBED, event.playlistId(), event)
        .whenComplete(
            (result, ex) -> {
              if (ex != null) {
                log.error("플레이리스트 구독 이벤트 발행 실패 eventId={}", event.eventId(), ex);
              }
            });
  }

  public void publishContentAdded(PlaylistContentAddedEvent event) {
    kafkaTemplate
        .send(KafkaTopics.PLAYLIST_CONTENT_ADDED, event.playlistId(), event)
        .whenComplete(
            (result, ex) -> {
              if (ex != null) {
                log.error("플레이리스트 콘텐츠 추가 이벤트 발행 실패 eventId={}", event.eventId(), ex);
              }
            });
  }
}
