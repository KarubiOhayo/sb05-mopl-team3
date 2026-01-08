package io.mopl.core.kafka;

public final class KafkaTopics {

  public static final String CONTENT_THUMBNAIL_REQUESTED = "content.thumbnail.requested";
  public static final String CONTENT_THUMBNAIL_REQUESTED_DLQ = "content.thumbnail.requested.dlq";
  public static final String CONTENT_THUMBNAIL_COMPLETED = "content.thumbnail.completed";
  public static final String CONTENT_THUMBNAIL_FAILED = "content.thumbnail.failed";

  private KafkaTopics() {}
}
