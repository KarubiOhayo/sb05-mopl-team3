package io.mopl.batch.tvseries;

import io.mopl.batch.client.tmdb.TmdbGenre;
import io.mopl.batch.client.tmdb.dto.TmdbTvSeriesResponse;
import io.mopl.batch.common.ContentDeduplicationTracker;
import io.mopl.batch.content.domain.Content;
import io.mopl.batch.content.domain.ContentRepository;
import io.mopl.batch.content.domain.ContentType;
import io.mopl.core.event.thumbnail.ThumbnailSourceType;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * TMDB TV 시리즈 응답을 {@link Content}로 변환하는 Processor.
 *
 * <p>중복 콘텐츠는 필터링하고, 썸네일 소스/태그를 채워 Writer로 전달한다.
 */
@Component
@StepScope
@RequiredArgsConstructor
public class TmdbTvSeriesItemProcessor implements ItemProcessor<TmdbTvSeriesResponse, Content> {

  private final ContentRepository contentRepository;
  private final ContentDeduplicationTracker deduplicationTracker;
  private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";

  /**
   * TMDB TV 시리즈 데이터를 콘텐츠로 변환한다.
   *
   * @param item TMDB TV 시리즈 응답
   * @return 변환된 콘텐츠, 중복이면 null
   */
  @Override
  public @Nullable Content process(TmdbTvSeriesResponse item) {
    String externalId = String.valueOf(item.getId());
    if (deduplicationTracker.isDuplicate(externalId, ContentType.TV_SERIES)) {
      return null;
    }
    if (contentRepository.existsByExternalIdAndType(externalId, ContentType.TV_SERIES)) {
      return null;
    }

    Content content =
        Content.builder()
            .id(null)
            .externalId(externalId)
            .title(item.getName())
            .description(item.getOverview() != null ? item.getOverview() : "")
            .thumbnailImageKey("")
            .type(ContentType.TV_SERIES)
            .build();

    String sourceThumbnailUrl =
        item.getPosterPath() != null ? IMAGE_BASE_URL + item.getPosterPath() : "";
    content.setSourceThumbnailUrl(sourceThumbnailUrl);
    content.setThumbnailSourceType(ThumbnailSourceType.TMDB);

    if (item.getGenreIds() != null) {
      List<String> tags =
          item.getGenreIds().stream().map(TmdbGenre::getNameById).collect(Collectors.toList());
      content.setTags(tags);
    }

    return content;
  }
}
