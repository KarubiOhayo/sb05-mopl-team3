package io.mopl.api.content.domain;

import io.mopl.api.content.dto.ContentPage;
import io.mopl.api.content.dto.ContentSearchRequest;
import io.mopl.api.content.dto.ContentSearchRow;

import java.util.List;

public interface ContentQueryRepository {

  List<ContentSearchRow> findAllForIndexing();

  ContentPage findContentPage(ContentSearchRequest request);

  long countContents(String typeEqual, String keywordLike, List<String> tagsIn);
}
