package io.mopl.batch.controller;

import io.mopl.batch.cleanup.BatchCleanupService;
import io.mopl.batch.cleanup.CleanupRequest;
import io.mopl.batch.cleanup.CleanupResult;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
@Validated
public class CleanupController {

  private final BatchCleanupService cleanupService;

  @PostMapping("/cleanup")
  public ResponseEntity<CleanupResult> cleanup(
      @RequestParam(required = false) String runId,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant to,
      @RequestParam(defaultValue = "true") boolean dryRun,
      @RequestParam(defaultValue = "false") boolean deleteS3,
      @RequestParam(defaultValue = "20") @Min(1) int sampleSize) {
    CleanupRequest request = new CleanupRequest(runId, from, to, dryRun, deleteS3, sampleSize);
    return ResponseEntity.ok(cleanupService.cleanup(request));
  }
}
