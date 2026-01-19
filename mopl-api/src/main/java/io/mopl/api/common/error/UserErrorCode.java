package io.mopl.api.common.error;

import io.mopl.core.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
  DUPLICATED_EMAIL(409, "error.user.duplicate-email"),
  USER_NOT_FOUND(404, "error.user.not-found"),
  UNAUTHORIZED(403, "error.user.unauthorized"),
  OAUTH_USER_CANNOT_CHANGE_PASSWORD(403, "error.auth.user-cannot-change-password"),
  CANNOT_UPDATE_OWN_ROLE(403, "error.user.cannot-update-own-role"),
  LAST_ADMIN_PROTECTION(403, "error.user.last-admin-protection");

  private final int status;
  private final String messageKey;
}
