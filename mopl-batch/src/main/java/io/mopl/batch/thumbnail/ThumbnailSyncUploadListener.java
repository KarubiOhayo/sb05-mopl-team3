package io.mopl.batch.thumbnail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 썸네일 요청 이벤트를 받아 배치 컨테이너에서 동기 업로드를 수행한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThumbnailSyncUploadListener {

  private final BatchThumbnailS3Uploader thumbnailS3Uploader;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ThumbnailRequestedSpringEvent event) {
    if (event.uploadMode() != ThumbnailUploadMode.SYNC) {
      return;
    }
    try {
      thumbnailS3Uploader.uploadFromUrl(event.sourceUrl(), event.s3Key());
    } catch (Exception ex) {
      log.error(
          "썸네일 업로드 실패: contentId={}, s3Key={}, sourceUrl={}",
          event.contentId(),
          event.s3Key(),
          event.sourceUrl(),
          ex);
    }
  }
}
