package io.mopl.batch.soccer;

import io.mopl.batch.client.tsdb.dto.TsdbSoccerResponse;
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
public class TsdbSoccerItemProcessor implements ItemProcessor<TsdbSoccerResponse, Content> {

  private final ContentRepository contentRepository;

  @Override
  public @Nullable Content process(TsdbSoccerResponse item) {
    if (contentRepository.existsByExternalIdAndType(
        String.valueOf(item.getId()), ContentType.SPORT)) {
      return null;
    }
    Content content =
        Content.builder()
            .id(null)
            .title(item.getTitle())
            .description(item.getDescription() != null ? item.getDescription() : "")
            .externalId(String.valueOf(item.getId()))
            .type(ContentType.SPORT)
            .thumbnailUrl("")
            .build();

    String sourceThumbnailUrl =
        item.getThumbnailUrl() != null ? item.getThumbnailUrl() + "/medium" : "";
    content.setSourceThumbnailUrl(sourceThumbnailUrl);
    content.setThumbnailSourceType(ThumbnailSourceType.THE_SPORTS_DB);

    if (content.getTags() != null) {
      content.getTags().add(item.getLeague());
      content.getTags().add(item.getVenue());
      content.getTags().add("Soccer");
      content.getTags().add("스포츠");
    }

    return content;
  }
}
