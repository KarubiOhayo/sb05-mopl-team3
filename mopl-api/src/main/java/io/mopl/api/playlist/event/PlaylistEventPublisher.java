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

  private <T> void publish(String topic, String key, T event, String eventId, String eventType) {
    kafkaTemplate
        .send(topic, key, event)
        .whenComplete(
            (result, ex) -> {
              if (ex != null) {
                log.error("{} event publish failed eventId={}", eventType, eventId, ex);
              }
            });
  }

  public void publishCreated(PlaylistCreatedEvent event) {
    publish(
        KafkaTopics.PLAYLIST_CREATED,
        event.playlistId(),
        event,
        event.eventId(),
        "playlist created");
  }

  public void publishSubscribed(PlaylistSubscribedEvent event) {
    publish(
        KafkaTopics.PLAYLIST_SUBSCRIBED,
        event.playlistId(),
        event,
        event.eventId(),
        "playlist subscribed");
  }

  public void publishContentAdded(PlaylistContentAddedEvent event) {
    publish(
        KafkaTopics.PLAYLIST_CONTENT_ADDED,
        event.playlistId(),
        event,
        event.eventId(),
        "playlist content added");
  }
}
