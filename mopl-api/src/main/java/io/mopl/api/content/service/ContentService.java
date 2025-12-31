package io.mopl.api.content.service;

import java.util.List;
import java.util.UUID;

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

	@Transactional(readOnly = true)
	public ContentDto findById(UUID contentId) {
		log.info("컨텐츠 단건 조회를 시작합니다. contentId: {}", contentId);

		Content content = contentRepository.findById(contentId)
			.orElse(null);

		if(content == null) { return null; }

		List<String> tagNames = contentTagRepository.findTagNamesByContentId(contentId);

		log.info("컨텐츠 조회를 완료했습니다. contentId: {}", contentId);
		return new ContentDto(content.getId(), content.getType(), content.getTitle(),
			content.getDescription(), content.getThumbnailUrl(), tagNames,
			0.0, 0, 0L);
	}

}
