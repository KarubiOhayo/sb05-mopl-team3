package io.mopl.api.follow.service;

import io.mopl.api.common.error.UserErrorCode;
import io.mopl.api.follow.domain.Follow;
import io.mopl.api.follow.dto.FollowDto;
import io.mopl.api.follow.dto.FollowRequest;
import io.mopl.api.follow.event.FollowCreatedInternalEvent;
import io.mopl.api.follow.repository.FollowRepository;
import io.mopl.api.user.service.UserService;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowService {

  private final FollowRepository followRepository;
  private final UserService userService;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public FollowDto create(@Valid FollowRequest request, UUID userId) {
    if (userId == null || request == null || request.getFolloweeId() == null) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }

    UUID followeeId = request.getFolloweeId();
    if (followeeId.equals(userId)) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }
    validateIdsAndFolloweeExists(followeeId, userId);

    try {
      Follow saved =
          followRepository.save(Follow.builder().followerId(userId).followeeId(followeeId).build());
      String followerName = userService.getUserName(userId);
      eventPublisher.publishEvent(new FollowCreatedInternalEvent(userId, followeeId, followerName));
      return toDto(saved);
    } catch (DataIntegrityViolationException e) {
      return followRepository
          .findByFollowerIdAndFolloweeId(userId, followeeId)
          .map(this::toDto)
          .orElseThrow(() -> new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR));
    }
  }

  @Transactional(readOnly = true)
  public boolean followedByMe(UUID followeeId, UUID userId) {
    validateIdsAndFolloweeExists(followeeId, userId);
    if (followeeId.equals(userId)) {
      return false;
    }
    return followRepository.findByFollowerIdAndFolloweeId(userId, followeeId).isPresent();
  }

  @Transactional(readOnly = true)
  public long count(UUID followeeId, UUID userId) {
    validateIdsAndFolloweeExists(followeeId, userId);
    return followRepository.countByFolloweeId(followeeId);
  }

  @Transactional
  public void cancel(UUID followId, UUID userId) {
    if (followId == null || userId == null) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }

    Follow follow =
        followRepository
            .findById(followId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));

    if (!follow.getFollowerId().equals(userId)) {
      throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }
    followRepository.deleteById(followId);
  }

  private FollowDto toDto(Follow follow) {
    return FollowDto.builder()
        .id(follow.getId())
        .followerId(follow.getFollowerId())
        .followeeId(follow.getFolloweeId())
        .build();
  }

  private void validateIdsAndFolloweeExists(UUID followeeId, UUID userId) {
    if (userId == null || followeeId == null) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
    }
    if (!userService.existsById(followeeId)) {
      throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
    }
  }
}
