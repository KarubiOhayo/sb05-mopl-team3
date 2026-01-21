package io.mopl.core.kafka;

public final class KafkaTopics {

  // ===== auth =====
  public static final String AUTH_PASSWORD_RESET = "auth.password.reset";

  // ===== content =====
  public static final String CONTENT_THUMBNAIL_REQUESTED = "content.thumbnail.requested";
  public static final String CONTENT_THUMBNAIL_REQUESTED_DLQ = "content.thumbnail.requested.dlq";
  public static final String CONTENT_THUMBNAIL_COMPLETED = "content.thumbnail.completed";
  public static final String CONTENT_THUMBNAIL_FAILED = "content.thumbnail.failed";
  public static final String CONTENT_AGGREGATE_UPDATED = "content.aggregate.updated";

  // ===== review =====
  public static final String REVIEW_CREATED = "review.created";
  public static final String REVIEW_DELETED = "review.deleted";
  public static final String REVIEW_UPDATED = "review.updated";

  // ===== user =====
  public static final String USER_FOLLOWED = "user.followed";
  public static final String USER_ROLE_CHANGED = "user.role.changed";

  // ===== dm =====
  public static final String DIRECT_MESSAGE_RECEIVED = "dm.received";
  // DM 대화 활성 상태 이벤트
  public static final String DIRECT_MESSAGE_CONVERSATION_ACTIVE =
      "direct-message.conversation.active";

  // ===== playlist =====
  public static final String PLAYLIST_CREATED = "playlist.created";
  public static final String PLAYLIST_SUBSCRIBED = "playlist.subscribed";
  public static final String PLAYLIST_CONTENT_ADDED = "playlist.content.added";

  // ===== notification =====
  public static final String NOTIFICATION_CREATED = "notification.created";

  // ===== watching =====
  public static final String WATCHING_SESSION_STARTED = "watching.session.started";

  // ===== conversation =====
  public static final String DIRECT_MESSAGE_SEND_REQUEST =
      "conversation.direct-message.send-request";
  public static final String DIRECT_MESSAGE_CREATED = "conversation.direct-message.created";

  private KafkaTopics() {}
}
