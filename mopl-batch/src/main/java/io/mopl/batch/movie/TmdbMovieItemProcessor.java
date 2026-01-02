package io.mopl.batch.movie;

import io.mopl.batch.client.tmdb.dto.TmdbMovieResponse;
import io.mopl.batch.content.domain.Content;
import io.mopl.batch.content.domain.ContentRepository;
import io.mopl.batch.content.domain.ContentType;
import io.mopl.core.event.thumbnail.ThumbnailSourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class TmdbMovieItemProcessor implements ItemProcessor<TmdbMovieResponse, Content> {

  private final ContentRepository contentRepository;
  private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";

  @Override
  public Content process(TmdbMovieResponse item) {
    // 중복 검사: 이미 존재하면 필터링 (Writer로 넘기지 않음)
    if (contentRepository.existsByExternalIdAndType(
        String.valueOf(item.getId()), ContentType.MOVIE)) {
      return null;
    }

    Content content =
        Content.builder()
            .id(null)
            .externalId(String.valueOf(item.getId()))
            .title(item.getTitle())
            .description(item.getOverview() != null ? item.getOverview() : "")
            .thumbnailUrl("")
            .type(ContentType.MOVIE)
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
