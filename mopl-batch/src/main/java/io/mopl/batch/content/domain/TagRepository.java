package io.mopl.batch.content.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 태그 엔티티 리포지토리. */
@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {
  /**
   * 이름으로 태그를 조회한다.
   *
   * @param name 태그 이름
   * @return 조회 결과
   */
  Optional<Tag> findByName(String name);
}
