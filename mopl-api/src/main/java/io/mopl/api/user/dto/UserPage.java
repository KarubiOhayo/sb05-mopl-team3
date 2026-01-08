package io.mopl.api.user.dto;

import io.mopl.api.user.domain.User;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserPage {

  private final List<User> users;
  private final boolean hasNext;
  private final String nextCursor;
  private final UUID nextIdAfter;
}
