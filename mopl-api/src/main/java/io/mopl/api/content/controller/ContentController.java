package io.mopl.api.content.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.mopl.api.content.dto.ContentDto;
import io.mopl.api.content.dto.ContentUpdateRequest;
import io.mopl.api.content.service.ContentService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/contents")
public class ContentController {

	private final ContentService contentService;

	@PatchMapping(value = "/{contentId}",
				consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ContentDto> update(
		@PathVariable("contentId") UUID contentId,
		@RequestPart("request") ContentUpdateRequest contentUpdateRequest,
		@RequestPart(value = "thumbnail", required = false)  MultipartFile thumbnail
	) {
		ContentDto updatedContent = contentService.update(contentId, contentUpdateRequest, thumbnail);
		return ResponseEntity
			.status(HttpStatus.OK)
			.body(updatedContent);
	}
}
