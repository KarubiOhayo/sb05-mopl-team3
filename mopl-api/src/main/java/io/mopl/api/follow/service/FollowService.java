package io.mopl.api.follow.service;

import io.mopl.api.follow.domain.Follow;
import io.mopl.api.follow.dto.FollowDto;
import io.mopl.api.follow.dto.FollowRequest;
import io.mopl.api.follow.repository.FollowRepository;
import io.mopl.api.user.service.UserService;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowService {

  private final FollowRepository followRepository;
  private final UserService userService;

  @Transactional
  public FollowDto create(@Valid FollowRequest request, UUID userId) {
    // null값 검증
    if (userId == null || request == null || request.getFolloweeId() == null) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }

    UUID followeeId = request.getFolloweeId();
    // 자신 팔로우 막기
    if (followeeId.equals(userId)) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }
    // followee 존재 확인
    userService.getUserSummary(followeeId);

    return followRepository
        .findByFollowerIdAndFolloweeId(userId, followeeId)
        .map(this::toDto)
        .orElseGet(
            () -> {
              Follow saved =
                  followRepository.save(
                      Follow.builder().followerId(userId).followeeId(followeeId).build());
              return toDto(saved);
            });
  }

  public boolean followedByMe(UUID followeeId, UUID userId) {
    validateUserAndFolloweeId(followeeId, userId);
    if (followeeId.equals(userId)) {
      return false;
    }
    return followRepository.findByFollowerIdAndFolloweeId(userId, followeeId).isPresent();
  }

  public long count(UUID followeeId, UUID userId) {
    validateUserAndFolloweeId(followeeId, userId);
    return followRepository.countByFolloweeId(followeeId);
  }

  public void cancel(UUID followId, UUID userId) {
    if (userId == null || followId == null) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }

    Follow follow =
        followRepository
            .findById(followId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN));

    if (!follow.getFollowerId().equals(userId)) {
      throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }
    followRepository.deleteById(followId);
  }

  // -- 헬퍼 메서드 --
  private FollowDto toDto(Follow follow) {
    return FollowDto.builder()
        .id(follow.getId())
        .followerId(follow.getFollowerId())
        .followeeId(follow.getFolloweeId())
        .build();
  }

  private void validateUserAndFolloweeId(UUID followeeId, UUID userId) {
    if (userId == null || followeeId == null) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }
    userService.getUserSummary(followeeId);
  }
}
