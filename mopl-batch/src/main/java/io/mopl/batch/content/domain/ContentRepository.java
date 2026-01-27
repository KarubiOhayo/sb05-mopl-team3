package io.mopl.batch.content.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

  List<Content> findAllByCreatedAtBetween(Instant from, Instant to);

  List<Content> findAllByThumbnailImageKeyStartingWith(String prefix);

  @Modifying
  @Transactional
  @Query("update Content c set c.watcherCount = 0 where c.watcherCount <> 0")
  int resetWatcherCounts();

  @Modifying
  @Transactional
  @Query("update Content c set c.watcherCount = :count where c.id = :id")
  int updateWatcherCount(@Param("id") UUID id, @Param("count") long count);

  @Modifying
  @Transactional
  @Query(
      value =
          "update contents c "
              + "left join ("
              + "  select content_id, count(*) as review_count, avg(rating) as average_rating "
              + "  from reviews "
              + "  group by content_id"
              + ") r on c.id = r.content_id "
              + "set c.review_count = coalesce(r.review_count, 0), "
              + "    c.average_rating = coalesce(r.average_rating, 0)",
      nativeQuery = true)
  int refreshReviewAggregates();

  @Modifying
  @Transactional
  @Query(
      value =
          "update contents c "
              + "left join ("
              + "  select content_id, count(*) as review_count, avg(rating) as average_rating "
              + "  from reviews "
              + "  where content_id in (:ids) "
              + "  group by content_id"
              + ") r on c.id = r.content_id "
              + "set c.review_count = coalesce(r.review_count, 0), "
              + "    c.average_rating = coalesce(r.average_rating, 0) "
              + "where c.id in (:ids)",
      nativeQuery = true)
  int refreshReviewAggregatesForContentIds(@Param("ids") List<String> contentIds);
}
