package io.mopl.api.playlist.domain;

import io.mopl.api.common.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "playlists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Playlist {

  @Id
  @Column(columnDefinition = "CHAR(36)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID id;

  @Column(name = "owner_id", nullable = false, columnDefinition = "CHAR(36)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID ownerId;

  @Column(nullable = false)
  private String title;

  @Lob
  @Column(nullable = false)
  private String description;

  @Column(name = "subscriber_count", nullable = false)
  @Builder.Default
  private long subscriberCount = 0L;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  public void generateId() {
    if (this.id == null) {
      this.id = UuidV7Generator.generate();
    }
  }
}
