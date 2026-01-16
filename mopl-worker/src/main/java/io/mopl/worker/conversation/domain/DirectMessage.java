package io.mopl.worker.conversation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "direct_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DirectMessage {

  @Id
  @Column(columnDefinition = "CHAR(36)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID id;

  @Column(name = "conversation_id", nullable = false, columnDefinition = "CHAR(36)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID conversationId;

  @Column(name = "sender_id", nullable = false, columnDefinition = "CHAR(36)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID senderId;

  @Column(name = "receiver_id", nullable = false, columnDefinition = "CHAR(36)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID receiverId;

  @Column(nullable = false, columnDefinition = "TEXT")
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  private String content;

  @Column(nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private SendingStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "read_at")
  private Instant readAt;

  public void markAsSent() {
    this.status = SendingStatus.SENT;
  }

  public void markAsFailed() {
    this.status = SendingStatus.FAILED;
  }

  @PrePersist
  public void generateId() {
    if (this.id == null) {
      this.id = UUID.randomUUID();
    }
    if (this.createdAt == null) {
      this.createdAt = Instant.now();
    }
    if (this.status == null) {
      this.status = SendingStatus.PENDING;
    }
  }
}
