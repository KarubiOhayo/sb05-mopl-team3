package io.mopl.batch.soccer;

import io.mopl.batch.client.tsdb.dto.TsdbSoccerResponse;
import io.mopl.batch.common.ContentDeduplicationTracker;
import io.mopl.batch.content.domain.Content;
import io.mopl.batch.content.domain.ContentRepository;
import io.mopl.batch.content.domain.ContentType;
import io.mopl.core.event.thumbnail.ThumbnailSourceType;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * TheSportsDB 축구 경기 응답을 {@link Content}로 변환하는 Processor.
 *
 * <p>중복 콘텐츠는 제외하고, 리그/경기장 정보를 태그로 구성한다.
 */
@Component
@StepScope
@RequiredArgsConstructor
public class TsdbSoccerItemProcessor implements ItemProcessor<TsdbSoccerResponse, Content> {

  private final ContentRepository contentRepository;
  private final ContentDeduplicationTracker deduplicationTracker;

  /**
   * TheSportsDB 축구 경기 데이터를 콘텐츠로 변환한다.
   *
   * @param item 축구 경기 응답
   * @return 변환된 콘텐츠, 중복이면 null
   */
  @Override
  public @Nullable Content process(TsdbSoccerResponse item) {
    String externalId = String.valueOf(item.getId());
    if (deduplicationTracker.isDuplicate(externalId, ContentType.SPORT)) {
      return null;
    }
    if (contentRepository.existsByExternalIdAndType(externalId, ContentType.SPORT)) {
      return null;
    }
    Content content =
        Content.builder()
            .id(null)
            .title(item.getTitle())
            .description(item.getDescription() != null ? item.getDescription() : "")
            .externalId(externalId)
            .type(ContentType.SPORT)
            .thumbnailImageKey("")
            .build();

    String sourceThumbnailUrl =
        item.getThumbnailUrl() != null ? item.getThumbnailUrl() + "/medium" : "";
    content.setSourceThumbnailUrl(sourceThumbnailUrl);
    content.setThumbnailSourceType(ThumbnailSourceType.THE_SPORTS_DB);

    content.setTags(new ArrayList<>());
    if (item.getLeague() != null && !item.getLeague().isBlank()) {
      content.getTags().add(item.getLeague());
    }
    if (item.getVenue() != null && !item.getVenue().isBlank()) {
      content.getTags().add(item.getVenue());
    }
    content.getTags().add("Soccer");

    return content;
  }
}
