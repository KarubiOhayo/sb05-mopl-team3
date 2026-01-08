package io.mopl.batch.content.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 콘텐츠-태그 조인 엔티티 리포지토리. */
@Repository
public interface ContentTagRepository extends JpaRepository<ContentTag, ContentTagId> {}
