package io.mopl.core.kafka;

public final class KafkaTopics {

  public static final String CONTENT_THUMBNAIL_REQUESTED = "content.thumbnail.requested";
  public static final String CONTENT_THUMBNAIL_REQUESTED_DLQ = "content.thumbnail.requested.dlq";
  public static final String CONTENT_THUMBNAIL_COMPLETED = "content.thumbnail.completed";
  public static final String CONTENT_THUMBNAIL_FAILED = "content.thumbnail.failed";
  public static final String USER_FOLLOWED = "user.followed";
  public static final String PLAYLIST_CREATED = "playlist.created";
  public static final String PLAYLIST_SUBSCRIBED = "playlist.subscribed";
  public static final String PLAYLIST_CONTENT_ADDED = "playlist.content.added";

  private KafkaTopics() {}
}
