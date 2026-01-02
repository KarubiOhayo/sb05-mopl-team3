package io.mopl.batch.content.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID> {
  Optional<Content> findByExternalIdAndType(String externalId, ContentType type);

  boolean existsByExternalIdAndType(String externalId, ContentType type);
}
