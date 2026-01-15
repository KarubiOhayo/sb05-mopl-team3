package io.mopl.api.content.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ContentElasticRepository extends ElasticsearchRepository<ContentDocument, UUID> {

	Optional<ContentDocument> findByContentId(UUID contentId);
	void deleteByContentId(UUID contentId);
}
