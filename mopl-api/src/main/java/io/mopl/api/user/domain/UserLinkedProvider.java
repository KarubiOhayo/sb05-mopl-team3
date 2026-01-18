package io.mopl.api.user.domain;

import io.mopl.api.common.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "user_linked_providers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserLinkedProvider {

  @Id
  @Column(columnDefinition = "CHAR(36)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID id;

  @Column(name = "user_id", nullable = false, columnDefinition = "CHAR(36)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", length = 20, nullable = false)
  private AuthProvider provider;

  @Column(name = "provider_user_id", nullable = false)
  private String providerUserId;

  @Column(name = "provider_email", length = 255)
  private String providerEmail;

  @CreatedDate
  @Column(name = "linked_at", nullable = false, updatable = false)
  private Instant linkedAt;

  @PrePersist
  public void generateId() {
    if (this.id == null) {
      this.id = UuidV7Generator.generate();
    }
  }

  public static UserLinkedProvider create(
      UUID userId, AuthProvider provider, String providerUserId, String providerEmail) {
    return UserLinkedProvider.builder()
        .userId(userId)
        .provider(provider)
        .providerUserId(providerUserId)
        .providerEmail(providerEmail)
        .build();
  }

  public boolean isSameProvider(AuthProvider provider) {
    return this.provider == provider;
  }
}
