package io.mopl.api.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "CHAR(36)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(length = 100)
  private String name;

  @Column(name = "password_hash", length = 255)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "auth_provider", length = 20)
  @Builder.Default
  private AuthProvider authProvider = AuthProvider.LOCAL;

  @Column(name = "provider_user_id", length = 255)
  private String providerUserId;

  @Column(name = "current_session_id")
  private String currentSessionId;

  @Column(nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private UserRole role;

  @Column(nullable = false)
  @Builder.Default
  private Boolean locked = false;

  @Column(name = "profile_image_url", length = 2048)
  private String profileImageUrl;

  @Column(name = "temp_password_hash", length = 255)
  private String tempPasswordHash;

  @Column(name = "temp_password_expires_at")
  private LocalDateTime tempPasswordExpiresAt;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

}
