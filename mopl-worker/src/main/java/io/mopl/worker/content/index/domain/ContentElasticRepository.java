package io.mopl.worker.content.index.domain;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ContentElasticRepository
    extends ElasticsearchRepository<ContentDocument, String> {}
