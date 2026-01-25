package io.mopl.worker.content.index;

import io.mopl.core.event.content.ContentIndexBatchRequestedEvent;
import io.mopl.core.kafka.KafkaTopics;
import io.mopl.worker.content.index.domain.ContentIndexQueryRepository;
import io.mopl.worker.content.index.dto.ContentIndexRow;
import io.mopl.worker.content.index.mapper.ContentIndexMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentIndexKafkaListener {

  private static final int BULK_SIZE = 10;

  private final ContentIndexQueryRepository queryRepository;
  private final ContentIndexMapper mapper;
  private final ContentElasticBulkService bulkService;

  @KafkaListener(
      topics = KafkaTopics.CONTENT_INDEX_REQUESTED,
      properties =
          "spring.json.value.default.type=io.mopl.core.event.content.ContentIndexBatchRequestedEvent")
  public void handle(ContentIndexBatchRequestedEvent event) {
    if (event.contentIds() == null || event.contentIds().isEmpty()) {
      return;
    }

    log.info("카프카 이벤트 수신: eventId={}, ", event.eventId());
    List<ContentIndexRow> rows = queryRepository.findAllForIndexing(event.contentIds());
    if (rows.isEmpty()) {
      log.debug("No content rows found for indexing. eventId={}", event.eventId());
      return;
    }

    log.info("ES bulk upsert 호출");
    bulkService.bulkUpsert(rows.stream().map(mapper::toDocument).toList(), BULK_SIZE);
  }
}
