package io.mopl.batch.content.domain;

import io.mopl.batch.common.UuidV7Generator;
import io.mopl.core.event.thumbnail.ThumbnailSourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 수집된 콘텐츠(영화/TV/스포츠)를 저장하는 엔티티.
 *
 * <p>썸네일 소스 URL, 썸네일 타입, 태그는 배치 처리 중에만 사용하는 임시 값이다.
 */
@Entity
@Table(name = "contents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Content {

  @Id
  @Column(columnDefinition = "CHAR(36)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID id;

  @Column(nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private ContentType type;

  @Column(name = "external_id")
  private String externalId;

  @Column(nullable = false)
  private String title;

  @Lob
  @Column(nullable = false)
  private String description;

  @Column(name = "thumbnail_url", nullable = false, length = 2048)
  @Setter
  private String thumbnailUrl;

  @Column(name = "average_rating", nullable = false)
  @Builder.Default
  private double averageRating = 0.0;

  @Column(name = "review_count", nullable = false)
  @Builder.Default
  private int reviewCount = 0;

  @Column(name = "watcher_count", nullable = false)
  @Builder.Default
  private long watcherCount = 0L;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Transient @Setter private String sourceThumbnailUrl;

  @Transient @Setter private ThumbnailSourceType thumbnailSourceType;

  @Transient @Setter private List<String> tags = new ArrayList<>();

  /**
   * 저장 전에 ID가 없다면 UUID v7로 생성한다.
   *
   * <p>배치 처리 단계에서 명시적으로 호출되기도 한다.
   */
  @PrePersist
  public void generateId() {
    if (this.id == null) {
      this.id = UuidV7Generator.generate();
    }
  }
}
