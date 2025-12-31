package io.mopl.api.content.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.mopl.api.content.dto.ContentCreateRequest;
import io.mopl.api.content.dto.ContentDto;
import io.mopl.api.content.service.ContentService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/contents")
public class ContentController {

	private final ContentService contentService;

	@GetMapping("/{contentId}")
	public ResponseEntity<ContentDto> findById(@PathVariable("contentId") UUID contentId) {
		return ResponseEntity.ok(contentService.findById(contentId));
	}
}
