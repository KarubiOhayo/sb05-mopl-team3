package io.mopl.api.user.service;

import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.dto.CursorResponseUserDto;
import io.mopl.api.user.dto.UserDto;
import io.mopl.api.user.dto.UserPage;
import io.mopl.api.user.dto.UserSearchRequest;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

  private final UserRepository userRepository;

  public CursorResponseUserDto findUsers(UserSearchRequest request) {

    String emailLike = request.getEmailLike();
    String roleEqual = request.getRoleEqual();
    Boolean isLocked = request.getIsLocked();

    String cursor = request.getCursor();
    var idAfter = request.getIdAfter();
    int limit = request.getLimitOrDefault();
    String sortDirection = request.getSortDirectionOrDefault();
    String sortBy = request.getSortByOrDefault();

    UserPage page =
        userRepository.findUsersPage(
            emailLike, roleEqual, isLocked, cursor, idAfter, limit, sortDirection, sortBy);

    List<User> users = page.getUsers();

    long totalCount = userRepository.countUsers(emailLike, roleEqual, isLocked);

    List<UserDto> data = new ArrayList<>();
    for (User user : users) {
      UserDto dto = UserDto.from(user);
      data.add(dto);
    }

    return CursorResponseUserDto.builder()
        .data(data)
        .nextCursor(page.getNextCursor())
        .nextIdAfter(page.getNextIdAfter())
        .hasNext(page.isHasNext())
        .totalCount(totalCount)
        .sortBy(sortBy)
        .sortDirection(sortDirection)
        .build();
  }
}
