package io.mopl.api.content.controller;

import io.mopl.api.content.dto.ContentDto;
import io.mopl.api.content.dto.ContentSearchRequest;
import io.mopl.api.content.dto.CursorResponseContentDto;
import io.mopl.api.content.service.ContentService;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contents")
public class ContentController {

  private final ContentService contentService;

  @GetMapping("/{contentId}")
  public ResponseEntity<ContentDto> findById(@PathVariable("contentId") UUID contentId) {
    return ResponseEntity.ok(contentService.findById(contentId));
  }

  @GetMapping
  public ResponseEntity<CursorResponseContentDto> findAll(@Valid @ModelAttribute ContentSearchRequest contentSearchRequest) {
    return ResponseEntity.ok(contentService.findAll(contentSearchRequest));
  }

  @DeleteMapping("/{contentId}")
  public ResponseEntity<Void> delete(@PathVariable("contentId") UUID contentId) {
    contentService.delete(contentId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
