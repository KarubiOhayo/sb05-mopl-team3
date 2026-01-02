package io.mopl.api.content.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentTagRepository extends JpaRepository<ContentTag, ContentTagId> {
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from ContentTag ct where ct.id.contentId = :contentId")
  void deleteByIdContentId(@Param("contentId") UUID contentId);
}
