package io.mopl.api.content.domain;

import java.util.List;

import io.mopl.api.content.dto.ContentPage;
import io.mopl.api.content.dto.ContentSearchRequest;

public interface ContentQueryRepository {
	ContentPage findContentPage(ContentSearchRequest request);

	long countContents(String typeEqual, String keywordLike, List<String> tagsIn);
}
