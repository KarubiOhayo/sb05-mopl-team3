package io.mopl.worker.content.index.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ContentElasticRepository extends ElasticsearchRepository<ContentDocument, String> {
  void deleteByContentIdIn(List<UUID> contentIds);
}
