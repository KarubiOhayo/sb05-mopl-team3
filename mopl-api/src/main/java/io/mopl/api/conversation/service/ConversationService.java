package io.mopl.api.conversation.service;

import io.mopl.api.common.error.ConversationErrorCode;
import io.mopl.api.common.error.UserErrorCode;
import io.mopl.api.content.service.ContentThumbnailUploadService;
import io.mopl.api.conversation.domain.Conversation;
import io.mopl.api.conversation.domain.ConversationParticipant;
import io.mopl.api.conversation.domain.ConversationParticipantId;
import io.mopl.api.conversation.domain.ConversationParticipantRepository;
import io.mopl.api.conversation.domain.ConversationRepository;
import io.mopl.api.conversation.domain.DirectMessageRepository;
import io.mopl.api.conversation.dto.ConversationDto;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.dto.UserSummary;
import io.mopl.core.error.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {
  private final ConversationRepository conversationRepository;
  private final UserRepository userRepository;
  private final ContentThumbnailUploadService contentThumbnailUploadService;
  private final ConversationParticipantRepository conversationParticipantRepository;
  private final DirectMessageRepository directMessageRepository;

  @Transactional
  public ConversationDto create(UUID userId, UUID withUserId) {
    if (userId == null) {
      log.warn("대화 생성 실패: 인증 사용자 ID가 없습니다.");
      throw new BusinessException(UserErrorCode.UNAUTHORIZED);
    }
    if (withUserId == null) {
      log.warn("대화 생성 실패: withUserId 누락. userId={}", userId);
      throw new BusinessException(ConversationErrorCode.WITH_USER_ID_REQUIRED);
    }
    if (userId.equals(withUserId)) {
      log.warn("대화 생성 실패: 자기 자신과의 대화 요청. userId={}", userId);
      throw new BusinessException(ConversationErrorCode.SAME_USER_NOT_ALLOWED);
    }

    conversationParticipantRepository
        .findConversationIdByParticipants(userId, withUserId)
        .ifPresent(
            existingConversationId -> {
              log.info(
                  "대화 생성 중단: 기존 대화 존재. userId={}, withUserId={}, conversationId={}",
                  userId,
                  withUserId,
                  existingConversationId);
              throw new BusinessException(ConversationErrorCode.CONVERSATION_ALREADY_EXISTS)
                  .addDetail("conversationId", existingConversationId.toString());
            });

    log.info("대화 생성 시작: userId={}, withUserId={}", userId, withUserId);
    User withUser =
        userRepository
            .findById(withUserId)
            .orElseThrow(
                () ->
                    new BusinessException(UserErrorCode.USER_NOT_FOUND)
                        .addDetail("withUserId", withUserId.toString()));

    String thumbnailUrl =
        contentThumbnailUploadService.generatePresignedUrl(withUser.getProfileImageUrl());

    UserSummary with =
        UserSummary.builder()
            .userId(withUser.getId())
            .name(withUser.getName())
            .profileImageUrl(thumbnailUrl)
            .build();

    Conversation conversation = conversationRepository.save(Conversation.builder().build());
    conversationRepository.flush();
    ConversationParticipantId userParticipantId =
        new ConversationParticipantId(conversation.getId(), userId);
    ConversationParticipantId withUserParticipantId =
        new ConversationParticipantId(conversation.getId(), withUser.getId());

    ConversationParticipant userParticipant =
        ConversationParticipant.builder()
            .id(userParticipantId)
            .joinedAt(null)
            .lastReadAt(null)
            .build();
    ConversationParticipant withUserParticipant =
        ConversationParticipant.builder()
            .id(withUserParticipantId)
            .joinedAt(null)
            .lastReadAt(null)
            .build();

    conversationParticipantRepository.save(userParticipant);
    conversationParticipantRepository.save(withUserParticipant);

    log.info(
        "대화 생성 완료: conversationId={}, userId={}, withUserId={}",
        conversation.getId(),
        userId,
        withUserId);
    return ConversationDto.builder()
        .id(conversation.getId())
        .with(with)
        .lastestMessage(null)
        .hasUnread(false)
        .build();
  }
}
