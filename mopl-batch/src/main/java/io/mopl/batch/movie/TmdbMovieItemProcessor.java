package io.mopl.batch.movie;

import io.mopl.batch.client.tmdb.TmdbGenre;
import io.mopl.batch.client.tmdb.dto.TmdbMovieResponse;
import io.mopl.batch.content.domain.Content;
import io.mopl.batch.content.domain.ContentRepository;
import io.mopl.batch.content.domain.ContentType;
import io.mopl.core.event.thumbnail.ThumbnailSourceType;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * TMDB 영화 응답을 {@link Content}로 변환하는 Processor.
 *
 * <p>이미 수집된 콘텐츠는 필터링하고, 썸네일 소스/태그를 채워 Writer로 전달한다.
 */
@Component
@StepScope
@RequiredArgsConstructor
public class TmdbMovieItemProcessor implements ItemProcessor<TmdbMovieResponse, Content> {

  private final ContentRepository contentRepository;
  private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";

  /**
   * TMDB 영화 데이터를 콘텐츠로 변환한다.
   *
   * @param item TMDB 영화 응답
   * @return 변환된 콘텐츠, 중복이면 null
   */
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
            .thumbnailImageKey("")
            .type(ContentType.MOVIE)
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
