package io.mopl.api.playlist.event;

import io.mopl.core.event.playlist.PlaylistContentAddedEvent;
import io.mopl.core.event.playlist.PlaylistCreatedEvent;
import io.mopl.core.event.playlist.PlaylistSubscribedEvent;
import io.mopl.core.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlaylistEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void publishCreated(PlaylistCreatedEvent event) {
    kafkaTemplate.send(KafkaTopics.PLAYLIST_CREATED, event.ownerId(), event);
  }

  public void publishSubscribed(PlaylistSubscribedEvent event) {
    kafkaTemplate.send(KafkaTopics.PLAYLIST_SUBSCRIBED, event.ownerId(), event);
  }

  public void publishContentAdded(PlaylistContentAddedEvent event) {
    kafkaTemplate.send(KafkaTopics.PLAYLIST_CONTENT_ADDED, event.playlistId(), event);
  }
}
