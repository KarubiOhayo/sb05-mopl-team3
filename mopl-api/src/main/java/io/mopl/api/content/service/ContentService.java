package io.mopl.api.content.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import io.mopl.api.content.domain.Content;
import io.mopl.api.content.domain.ContentRepository;
import io.mopl.api.content.domain.ContentTag;
import io.mopl.api.content.domain.ContentTagId;
import io.mopl.api.content.domain.ContentTagRepository;
import io.mopl.api.content.domain.Tag;
import io.mopl.api.content.domain.TagRepository;
import io.mopl.api.content.dto.ContentCreateRequest;
import io.mopl.api.content.dto.ContentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

	private final ContentRepository contentRepository;
	private final ContentTagRepository contentTagRepository;
	private final TagRepository tagRepository;

	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public ContentDto create(ContentCreateRequest contentCreateRequest, MultipartFile thumbnail) {
		log.info("컨텐츠 생성을 시작합니다.");
		String title = contentCreateRequest.getTitle();
		String description = contentCreateRequest.getDescription();

		Content content = Content.builder()
			.type(contentCreateRequest.getType())
			.title(title)
			.description(description)
			// null 대신 dml에 있는 thumbnail_url 주소 아무거나 넣기
			.thumbnailUrl(null)
			.build();
		contentRepository.save(content);

		List<String> tagNames = contentCreateRequest.getTags();

		for (String tagName : tagNames) {
			Tag newTag = tagRepository.findByName(tagName)
				.orElseGet(()-> tagRepository.save(new Tag(tagName)));

			ContentTag contentTag = ContentTag.builder()
				.id(new ContentTagId(content.getId(), newTag.getId()))
				.build();

			contentTagRepository.save(contentTag);
		}

		log.info("컨텐츠 생성을 완료했습니다.");
		return new ContentDto(content.getId(), content.getType(), content.getTitle(),
			content.getDescription(), content.getThumbnailUrl(), tagNames,
			0.0, 0, 0L);
	}
}
