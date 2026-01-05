package io.mopl.batch.tvseries;

import io.mopl.batch.client.tmdb.dto.TmdbTvSeriesResponse;
import io.mopl.batch.content.domain.Content;
import io.mopl.batch.content.domain.ContentRepository;
import io.mopl.batch.content.domain.ContentType;
import io.mopl.core.event.thumbnail.ThumbnailSourceType;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class TmdbTvSeriesItemProcessor implements ItemProcessor<TmdbTvSeriesResponse, Content> {

  private final ContentRepository contentRepository;
  private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";

  @Override
  public @Nullable Content process(TmdbTvSeriesResponse item) {
    if (contentRepository.existsByExternalIdAndType(
        String.valueOf(item.getId()), ContentType.TV_SERIES)) {
      return null;
    }

    Content content =
        Content.builder()
            .id(null)
            .externalId(String.valueOf(item.getId()))
            .title(item.getName())
            .description(item.getOverview() != null ? item.getOverview() : "")
            .thumbnailUrl("")
            .type(ContentType.TV_SERIES)
            .build();

    String sourceThumbnailUrl =
        item.getPosterPath() != null ? IMAGE_BASE_URL + item.getPosterPath() : "";
    content.setSourceThumbnailUrl(sourceThumbnailUrl);
    content.setThumbnailSourceType(ThumbnailSourceType.TMDB);

    if (item.getGenreIds() != null) {
      content.setGenreIds(item.getGenreIds());
    }

    return content;
  }
}
