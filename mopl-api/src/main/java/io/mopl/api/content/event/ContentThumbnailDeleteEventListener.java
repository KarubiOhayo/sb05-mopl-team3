package io.mopl.api.content.event;

import io.mopl.api.content.service.ContentThumbnailUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentThumbnailDeleteEventListener {

  private final ContentThumbnailUploadService contentThumbnailUploadService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ContentThumbnailDeleteEvent event) {
    try {
      contentThumbnailUploadService.deleteThumbnail(event.getDeletedUrl());
    } catch (Exception e) {
      log.error("기존 썸네일 삭제가 실패하였습니다. key = {}", event.getDeletedUrl(), e);
    }
  }
}
