package io.mopl.worker.conversation.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

  @Modifying(clearAutomatically = true)
  @Query(
      "UPDATE DirectMessage dm SET dm.status = :sentStatus WHERE dm.id = :dmId AND dm.status = :pendingStatus")
  int updateStatus(
      @Param("dmId") UUID dmId,
      @Param("sentStatus") SendingStatus sentStatus,
      @Param("pendingStatus") SendingStatus pendingStatus);

  default int updateStatusToSentIfPending(UUID dmId) {
    return updateStatus(dmId, SendingStatus.SENT, SendingStatus.PENDING);
  }
}
