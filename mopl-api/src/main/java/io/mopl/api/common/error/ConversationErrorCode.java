package io.mopl.api.common.error;

import io.mopl.core.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ConversationErrorCode implements ErrorCode {
  SAME_USER_NOT_ALLOWED(400, "error.conversation.same-user"),
  WITH_USER_ID_REQUIRED(400, "error.conversation.with-user-id-required"),
  CONVERSATION_NOT_FOUND(404, "error.conversation.not-found"),
  CONVERSATION_PARTICIPANT_NOT_FOUND(404, "error.conversation.participant-not-found"),
  CONVERSATION_ALREADY_EXISTS(409, "error.conversation.already-exists");

  private final int status;
  private final String messageKey;
}
