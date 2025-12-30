package io.mopl.batch.common.writer;

import io.mopl.batch.client.tmdb.TmdbGenre;
import io.mopl.batch.content.domain.Content;
import io.mopl.batch.content.domain.ContentRepository;
import io.mopl.batch.content.domain.ContentTag;
import io.mopl.batch.content.domain.ContentTagId;
import io.mopl.batch.content.domain.ContentTagRepository;
import io.mopl.batch.content.domain.Tag;
import io.mopl.batch.content.domain.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentWithTagWriter implements ItemWriter<Content> {

  private final ContentRepository contentRepository;
  private final TagRepository tagRepository;
  private final ContentTagRepository contentTagRepository;

  @Override
  @Transactional
  public void write(Chunk<? extends Content> chunk) {
    for (Content content : chunk) {
      // 1. 저장 (Processor에서 중복은 이미 걸러짐)
      Content savedContent = contentRepository.save(content);

      // 2. Tag 저장 및 연결
      if (savedContent.getGenreIds() != null) {
        for (Integer genreId : savedContent.getGenreIds()) {
          String tagName = TmdbGenre.getNameById(genreId);

          // 태그가 없으면 생성, 있으면 조회
          Tag tag =
              tagRepository
                  .findByName(tagName)
                  .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));

          // ContentTag 연결
          ContentTag contentTag =
              ContentTag.builder().id(new ContentTagId(savedContent.getId(), tag.getId())).build();

          contentTagRepository.save(contentTag);
        }
      }
    }
  }
}
