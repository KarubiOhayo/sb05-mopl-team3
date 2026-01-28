package io.mopl.api.content.init;

import io.mopl.api.content.domain.ContentDocument;
import io.mopl.api.content.domain.ContentElasticRepository;
import io.mopl.api.content.domain.ContentRepository;
import io.mopl.api.content.dto.ContentSearchRow;
import io.mopl.api.content.mapper.ContentMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "search.enabled", havingValue = "true", matchIfMissing = true)
public class ContentElasticInitInitializer {

  private final ContentRepository contentRepository;
  private final ContentElasticRepository contentElasticRepository;
  private final ContentMapper contentMapper;
  private final ElasticsearchOperations elasticsearchOperations;

  @Value("${search.index.reset-on-startup:false}")
  private boolean resetOnStartup;

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void init() {
    IndexOperations indexOps = elasticsearchOperations.indexOps(ContentDocument.class);
    boolean indexExists = indexOps.exists();
    log.info("Elastic index init start: index=contents exists={}", indexExists);
    if (!indexExists) {
      try {
        indexOps.create();
        log.info("Elastic index created: index=contents");
      } catch (RuntimeException ex) {
        if (!isAlreadyExists(ex)) {
          throw ex;
        }
        log.warn("Elastic index already exists (race). Continue applying mapping: index=contents");
      }
      indexOps.putMapping(indexOps.createMapping(ContentDocument.class));
      log.info("Elastic mapping applied: index=contents");
    }

    if (resetOnStartup && indexExists) {
      contentElasticRepository.deleteAll();
      log.info("Elastic index reset: index=contents");
    }

    if (!indexExists || resetOnStartup) {
      List<ContentSearchRow> rows = contentRepository.findAllForIndexing();

      List<ContentDocument> documents =
          rows.stream().map(contentMapper::toContentDocument).toList();

      contentElasticRepository.saveAll(documents);
      log.info("Elastic index seeded: index=contents count={}", documents.size());
    }
  }

  private boolean isAlreadyExists(Throwable ex) {
    Throwable current = ex;
    while (current != null) {
      String message = current.getMessage();
      if (message != null && message.contains("resource_already_exists_exception")) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
