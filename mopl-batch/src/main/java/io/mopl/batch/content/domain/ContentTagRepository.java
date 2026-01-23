package io.mopl.batch.content.domain;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 콘텐츠-태그 조인 엔티티 리포지토리. */
@Repository
public interface ContentTagRepository extends JpaRepository<ContentTag, ContentTagId> {
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from ContentTag ct where ct.id.contentId in :contentIds")
  int deleteByContentIds(@Param("contentIds") Collection<UUID> contentIds);
}
