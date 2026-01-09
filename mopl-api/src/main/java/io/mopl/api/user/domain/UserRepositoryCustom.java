package io.mopl.api.user.domain;

import io.mopl.api.user.dto.UserPage;
import java.util.UUID;

public interface UserRepositoryCustom {

  /** 사용자 목록을 커서 기반 페이지네이션으로 조회 */
  UserPage findUsersPage(
      String emailLike,
      String roleEqual,
      Boolean isLocked,
      String cursor,
      UUID idAfter,
      int limit,
      String sortDirection,
      String sortBy);

  /** 필터 조건에 해당하는 사용자 총 수 조회 */
  long countUsers(String emailLike, String roleEqual, Boolean isLocked);
}
