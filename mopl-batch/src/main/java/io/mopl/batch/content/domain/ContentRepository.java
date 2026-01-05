package io.mopl.batch.content.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 콘텐츠 엔티티에 대한 JPA 리포지토리. */
@Repository
public interface ContentRepository extends JpaRepository<Content, UUID> {
  /**
   * 외부 ID와 타입으로 콘텐츠를 조회한다.
   *
   * @param externalId 외부 시스템 ID
   * @param type 콘텐츠 타입
   * @return 조회 결과
   */
  Optional<Content> findByExternalIdAndType(String externalId, ContentType type);

  /**
   * 외부 ID와 타입으로 콘텐츠 존재 여부를 확인한다.
   *
   * @param externalId 외부 시스템 ID
   * @param type 콘텐츠 타입
   * @return 존재 여부
   */
  boolean existsByExternalIdAndType(String externalId, ContentType type);
}
