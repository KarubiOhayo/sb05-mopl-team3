package io.mopl.batch.content.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentTagRepository extends JpaRepository<ContentTag, ContentTagId> {}
