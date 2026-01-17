package io.mopl.api.content.init;

import io.mopl.api.content.domain.ContentDocument;
import io.mopl.api.content.domain.ContentElasticRepository;
import io.mopl.api.content.domain.ContentRepository;
import io.mopl.api.content.dto.ContentSearchRow;
import io.mopl.api.content.mapper.ContentMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ContentElasticInitInitializer {

  private final ContentRepository contentRepository;
  private final ContentElasticRepository contentElasticRepository;
  private final ContentMapper contentMapper;

  @EventListener(ApplicationReadyEvent.class)
  @Transactional(readOnly = true)
  public void init() {
    contentElasticRepository.deleteAll();

    List<ContentSearchRow> rows = contentRepository.findAllForIndexing();

    List<ContentDocument> documents = rows.stream().map(contentMapper::toContentDocument).toList();

    contentElasticRepository.saveAll(documents);
  }
}
