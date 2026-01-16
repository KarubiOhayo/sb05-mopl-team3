package io.mopl.api.user.mapper;

import io.mopl.api.user.domain.User;
import io.mopl.api.user.dto.UserSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

  @Mapping(target = "profileImageUrl", source = "presignedProfileImageUrl")
  @Mapping(target = "userId", source = "user.id")
  UserSummary toSummary(User user, String presignedProfileImageUrl);
}
