package io.mopl.api.content.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import io.mopl.api.content.domain.ContentDocument;
import io.mopl.api.content.domain.ContentType;
import io.mopl.api.content.dto.ContentDto;
import io.mopl.api.content.dto.ContentSearchRequest;
import io.mopl.api.content.dto.CursorResponseContentDto;
import io.mopl.api.content.mapper.ContentMapper;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentSearchService {

  private final ContentThumbnailUploadService contentThumbnailUploadService;
  private final ElasticsearchOperations elasticsearchOperations;
  private final ContentMapper mapper;

  @Transactional(readOnly = true)
  public ContentDto findById(UUID contentId) {
    log.info("ES 컨텐츠 단건 조회 시작 contentId={}", contentId);

    Query termQuery = Query.of(q -> q.term(t -> t.field("contentId").value(contentId.toString())));

    NativeQuery query = NativeQuery.builder().withQuery(termQuery).build();

    SearchHits<ContentDocument> hits = elasticsearchOperations.search(query, ContentDocument.class);

    ContentDocument doc =
        hits.getSearchHits().stream()
            .map(SearchHit::getContent)
            .findFirst()
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));

    log.info("ES 컨텐츠 단건 조회 완료 contentId={}", contentId);

    String thumbnailImageKey =
        contentThumbnailUploadService.generatePresignedUrl(doc.getThumbnailImageKey());
    doc.setThumbnailImageKey(thumbnailImageKey);

    return mapper.toContentDto(doc);
  }

  @Transactional(readOnly = true)
  public CursorResponseContentDto findAll(ContentSearchRequest contentSearchRequest) {
    String sortBy = contentSearchRequest.getSortByOrDefault();
    String sortDirection = contentSearchRequest.getSortDirectionOrDefault();

    SortBy sortByEnum = SortBy.from(sortBy);
    SortDirection sortDirectionEnum = SortDirection.from(sortDirection);
    int limit = contentSearchRequest.getLimitOrDefault();

    NativeQueryBuilder queryBuilder =
        NativeQuery.builder()
            .withQuery(buildSearchQuery(contentSearchRequest))
            .withTrackTotalHits(true)
            .withPageable(PageRequest.of(0, limit + 1))
            .withSort(
                s ->
                    s.field(
                        f ->
                            f.field(resolveSortField(sortByEnum))
                                .order(resolveSortOrder(sortDirectionEnum))))
            .withSort(
                s -> s.field(f -> f.field("contentId").order(resolveSortOrder(sortDirectionEnum))));

    applySearchAfter(queryBuilder, contentSearchRequest, sortByEnum);

    SearchHits<ContentDocument> hits =
        elasticsearchOperations.search(queryBuilder.build(), ContentDocument.class);

    long totalCount = hits.getTotalHits();

    List<ContentDocument> documents =
        hits.getSearchHits().stream().map(SearchHit::getContent).toList();

    boolean hasNext = documents.size() > limit;
    if (hasNext) {
      documents = documents.subList(0, limit);
    }

    List<ContentDto> data =
        documents.stream()
            .map(
                doc ->
                    new ContentDto(
                        doc.getContentId(),
                        doc.getType(),
                        doc.getTitle(),
                        doc.getDescription(),
                        contentThumbnailUploadService.generatePresignedUrl(
                            doc.getThumbnailImageKey()),
                        doc.getTags() != null ? doc.getTags() : List.of(),
                        doc.getAverageRating(),
                        doc.getReviewCount(),
                        doc.getWatcherCount()))
            .toList();

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !documents.isEmpty()) {
      ContentDocument last = documents.getLast();
      nextIdAfter = last.getContentId();
      if (sortByEnum == SortBy.CREATED_AT) {
        nextCursor = String.valueOf(last.getCreatedAt());
      } else if (sortByEnum == SortBy.RATE) {
        nextCursor = String.valueOf(last.getAverageRating());
      } else {
        nextCursor = String.valueOf(last.getWatcherCount());
      }
    }

    return CursorResponseContentDto.builder()
        .data(data)
        .nextCursor(nextCursor)
        .nextIdAfter(nextIdAfter)
        .hasNext(hasNext)
        .totalCount(totalCount)
        .sortBy(sortBy)
        .sortDirection(sortDirection)
        .build();
  }

  private Query buildSearchQuery(ContentSearchRequest request) {
    return Query.of(
        q ->
            q.bool(
                b -> {
                  if (request.getTypeEqual() != null && !request.getTypeEqual().isBlank()) {
                    ContentType type;
                    try {
                      type = ContentType.fromValue(request.getTypeEqual());
                    } catch (IllegalArgumentException e) {
                      throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
                          .addDetail("reason", "유효하지 않은 typeEqual 값입니다.")
                          .addDetail("typeEqual", request.getTypeEqual());
                    }
                    b.filter(f -> f.term(t -> t.field("type").value(type.name())));
                  }

                  // keywordLike가 공백이면 조건 제외
                  String keyword = request.getKeywordLike();
                  if (keyword != null && !keyword.isBlank()) {
                    String pattern = "*" + escapeWildcard(keyword) + "*";
                    b.must(
                        m ->
                            m.bool(
                                bb -> {
                                  bb.should(
                                      s ->
                                          s.wildcard(
                                              w ->
                                                  w.field("title.keyword")
                                                      .value(pattern)
                                                      .caseInsensitive(true)));
                                  bb.should(
                                      s ->
                                          s.wildcard(
                                              w ->
                                                  w.field("description.keyword")
                                                      .value(pattern)
                                                      .caseInsensitive(true)));
                                  bb.minimumShouldMatch("1");
                                  return bb;
                                }));
                  }

                  // tagsIn이 비어있으면 태그 필터 제외
                  List<String> tags = request.getTagsIn();
                  if (tags != null && !tags.isEmpty()) {
                    for (String tag :
                        tags.stream().filter(t -> t != null && !t.isBlank()).distinct().toList()) {
                      b.filter(f -> f.term(t -> t.field("tags").value(tag)));
                    }
                  }

                  return b;
                }));
  }

  private void applySearchAfter(
      NativeQueryBuilder queryBuilder, ContentSearchRequest request, SortBy sortBy) {
    String cursor = request.getCursor();
    if (cursor == null || cursor.isBlank()) {
      return;
    }
    if (request.getIdAfter() == null) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
          .addDetail("reason", "cursor가 있으면 idAfter는 필수이다.");
    }

    List<Object> values = new ArrayList<>();
    if (sortBy == SortBy.CREATED_AT) {
      try {
        Instant time = Instant.parse(cursor);
        values.add(JsonData.of(time.toString()));
      } catch (DateTimeParseException e) {
        throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
            .addDetail("reason", "잘못된 cursor 형식입니다.")
            .addDetail("cursor", cursor);
      }
    } else if (sortBy == SortBy.RATE) {
      try {
        values.add(JsonData.of(Double.parseDouble(cursor)));
      } catch (NumberFormatException e) {
        throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
            .addDetail("reason", "잘못된 cursor 형식입니다.")
            .addDetail("cursor", cursor);
      }
    } else {
      try {
        values.add(JsonData.of(Long.parseLong(cursor)));
      } catch (NumberFormatException e) {
        throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
            .addDetail("reason", "잘못된 cursor 형식입니다.")
            .addDetail("cursor", cursor);
      }
    }

    values.add(JsonData.of(request.getIdAfter().toString()));
    queryBuilder.withSearchAfter(values);
  }

  private String resolveSortField(SortBy sortBy) {
    if (sortBy == SortBy.CREATED_AT) {
      return "createdAt";
    }
    if (sortBy == SortBy.RATE) {
      return "averageRating";
    }
    return "watcherCount";
  }

  private SortOrder resolveSortOrder(SortDirection direction) {
    return (direction == SortDirection.DESC) ? SortOrder.Desc : SortOrder.Asc;
  }

  private String escapeWildcard(String value) {
    return value.replace("\\", "\\\\").replace("*", "\\*").replace("?", "\\?");
  }

  private enum SortBy {
    CREATED_AT,
    WATCHER_COUNT,
    RATE;

    static SortBy from(String from) {
      return switch (from) {
        case "createdAt" -> CREATED_AT;
        case "rate" -> RATE;
        case "watcherCount" -> WATCHER_COUNT;
        default ->
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
                .addDetail("reason", "유효하지 않은 sortBy 값입니다.")
                .addDetail("sortBy", from);
      };
    }
  }

  private enum SortDirection {
    ASC,
    DESC;

    static SortDirection from(String direction) {
      return switch (direction) {
        case "ASCENDING" -> ASC;
        case "DESCENDING" -> DESC;
        default ->
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
                .addDetail("reason", "유효하지 않은 sortDirection 값입니다.")
                .addDetail("direction", direction);
      };
    }
  }
}
