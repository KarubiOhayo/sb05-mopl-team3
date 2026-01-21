package io.mopl.api.review.domain;

import io.mopl.api.common.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Review {

  @Id
  @Column(columnDefinition = "CHAR(36)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID id;

  @Column(name = "content_id", nullable = false, columnDefinition = "CHAR(36)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID contentId;

  @Column(name = "author_id", nullable = false, columnDefinition = "CHAR(36)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID authorId;

  @Column(nullable = false, length = 2000)
  private String text;

  @Column(nullable = false)
  private double rating;

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

  public void update(String text, double rating) {

    if (text != null) {
      this.text = text;
    }
    if (rating != 0) {
      this.rating = rating;
    }
  }
}
