package io.mopl.api.content.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
import io.mopl.api.content.dto.ContentDto;
import io.mopl.api.content.dto.ContentUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

	private final ContentRepository contentRepository;
	private final ContentTagRepository contentTagRepository;
	private final TagRepository tagRepository;

	@PreAuthorize("hasRole('ADMIN')")
	@Transactional
	public ContentDto update(UUID contentId, ContentUpdateRequest contentUpdateRequest, MultipartFile thumbnail) {
		log.debug("컨텐츠 수정 시작: contentId={}, request = {}", contentId, contentUpdateRequest);

		Content content = contentRepository.findById(contentId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 contentId 값입니다."));

		String title = contentUpdateRequest.getTitle();
		String description = contentUpdateRequest.getDescription();

		content.update(title, description, null);

		List<String> requestedTags = contentUpdateRequest.getTags();
		if (requestedTags != null) {
			Set<String> requested = new HashSet<>(requestedTags);
			Set<String> existing = new HashSet<>(contentTagRepository.findTagNamesByContentId(contentId));

			Set<String> toRemove = new HashSet<>(existing);
			toRemove.removeAll(requested);

			Set<String> toAdd = new HashSet<>(requested);
			toAdd.removeAll(existing);

			if (!toRemove.isEmpty()) {
				List<Tag> removeTags = tagRepository.findByNameIn(toRemove);
				List<UUID> removeTagIds = removeTags.stream().map(Tag::getId).toList();
				if(!removeTagIds.isEmpty()) {
					contentTagRepository.deleteByContentIdAndTagIdIn(contentId, removeTagIds);
				}
			}

			if (!toAdd.isEmpty()) {
				List<Tag> existingTags = tagRepository.findByNameIn(toAdd);
				Map<String, Tag> tagByName = existingTags.stream()
					.collect(Collectors.toMap(Tag::getName, t -> t));

				for(String tagName : toAdd) {
					Tag tag = tagByName.get(tagName);
					if(tag == null) {
						tag = tagRepository.save(new Tag(tagName));
					}
					contentTagRepository.save(ContentTag.builder()
						.id(new ContentTagId(contentId, tag.getId()))
						.build());
				}
			}
		}

		log.info("컨텐츠 수정을 완료하였습니다. contentId: {}", contentId);
		return new ContentDto(content.getId(), content.getType(), content.getTitle(),
			content.getDescription(), content.getThumbnailUrl(), requestedTags,
			0.0, 0, 0L);
	}
}
