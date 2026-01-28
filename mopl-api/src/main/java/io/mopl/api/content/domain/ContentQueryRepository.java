package io.mopl.api.content.domain;

import io.mopl.api.content.dto.ContentPage;
import io.mopl.api.content.dto.ContentSearchRequest;
import io.mopl.api.content.dto.ContentSearchRow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentQueryRepository {

  List<ContentSearchRow> findAllForIndexing();

  List<ContentSearchRow> findBatchForIndexing(UUID lastId, int limit);

  Optional<ContentSearchRow> findOneForIndexing(UUID contentId);

  ContentPage findContentPage(ContentSearchRequest request);

  long countContents(String typeEqual, String keywordLike, List<String> tagsIn);
}
