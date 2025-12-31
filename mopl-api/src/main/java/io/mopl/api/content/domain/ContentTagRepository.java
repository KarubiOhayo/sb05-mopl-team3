package io.mopl.api.content.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentTagRepository extends JpaRepository<ContentTag, ContentTagId> {

	@Query("select t.name from ContentTag ct join Tag t on ct.id.tagId = t.id " +
		"where ct.id.contentId = :contentId")
	List<String> findTagNamesByContentId(@Param("contentId") UUID contentId);
}
