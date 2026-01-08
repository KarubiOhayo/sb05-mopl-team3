package io.mopl.api.content.domain;

import io.mopl.api.content.dto.ContentPage;
import io.mopl.api.content.dto.ContentSearchRequest;
import java.util.List;

public interface ContentQueryRepository {
  ContentPage findContentPage(ContentSearchRequest request);

  long countContents(String typeEqual, String keywordLike, List<String> tagsIn);
}
