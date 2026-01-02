package io.mopl.api.content.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.mopl.api.content.service.ContentService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contents")
public class ContentController {

	private final ContentService contentService;

	@DeleteMapping("/{contentId}")
	public ResponseEntity<Void> delete(@PathVariable("contentId") UUID contentId) {
		contentService.delete(contentId);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}
}
