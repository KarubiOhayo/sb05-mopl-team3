package io.mopl.worker.content.index;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import io.mopl.worker.content.index.domain.ContentDocument;
import io.mopl.worker.content.index.domain.ContentElasticRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentElasticBulkService {

  private final ElasticsearchOperations elasticsearchOperations;
  private final ContentElasticRepository contentElasticRepository;

  public void bulkUpsert(List<ContentDocument> documents, int bulkSize) {
    if (documents == null || documents.isEmpty()) {
      return;
    }
    if (bulkSize <= 0) {
      bulkSize = documents.size();
    }

    for (int i = 0; i < documents.size(); i += bulkSize) {
      int end = Math.min(i + bulkSize, documents.size());
      List<ContentDocument> batch = new ArrayList<>(documents.subList(i, end));

      Map<UUID, String> existingIds = findExistingIds(batch);
      for (ContentDocument doc : batch) {
        String existingId = existingIds.get(doc.getContentId());
        if (existingId != null && !existingId.isBlank()) {
          doc.setId(existingId);
        }
      }

      contentElasticRepository.saveAll(batch);
    }
  }

  private Map<UUID, String> findExistingIds(List<ContentDocument> batch) {
    List<FieldValue> values =
        batch.stream()
            .map(ContentDocument::getContentId)
            .filter(id -> id != null)
            .map(id -> FieldValue.of(id.toString()))
            .toList();
    if (values.isEmpty()) {
      return Map.of();
    }

    Query termsQuery =
        Query.of(q -> q.terms(t -> t.field("contentId").terms(ts -> ts.value(values))));
    NativeQuery query =
        NativeQuery.builder()
            .withQuery(termsQuery)
            .withPageable(PageRequest.of(0, values.size()))
            .build();

    SearchHits<ContentDocument> hits = elasticsearchOperations.search(query, ContentDocument.class);

    Map<UUID, String> existingIds = new HashMap<>();
    for (SearchHit<ContentDocument> hit : hits.getSearchHits()) {
      ContentDocument doc = hit.getContent();
      if (doc.getContentId() != null && doc.getId() != null) {
        existingIds.put(doc.getContentId(), doc.getId());
      }
    }

    return existingIds;
  }

  public void deleteByContentIds(List<UUID> contentIds) {
    if (contentIds == null || contentIds.isEmpty()) {
      return;
    }
    contentElasticRepository.deleteByContentIdIn(contentIds);
  }
}
