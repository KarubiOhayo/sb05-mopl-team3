package io.mopl.batch.content.domain;

import io.mopl.batch.common.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 콘텐츠에 부여되는 태그 엔티티. */
@Entity
@Table(name = "tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Tag {

  @Id
  @Column(columnDefinition = "CHAR(36)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID id;

  @Column(nullable = false, length = 100)
  private String name;

  /**
   * 저장 전에 ID가 없다면 UUID v7로 생성한다.
   *
   * <p>태그 생성 시점에만 호출된다.
   */
  @PrePersist
  public void generateId() {
    if (this.id == null) {
      this.id = UuidV7Generator.generate();
    }
  }
}
