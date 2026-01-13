package io.mopl.api.conversation.service;

import io.mopl.api.common.error.ConversationErrorCode;
import io.mopl.api.common.error.UserErrorCode;
import io.mopl.api.content.service.ContentThumbnailUploadService;
import io.mopl.api.conversation.domain.Conversation;
import io.mopl.api.conversation.domain.ConversationParticipant;
import io.mopl.api.conversation.domain.ConversationParticipantId;
import io.mopl.api.conversation.domain.ConversationParticipantRepository;
import io.mopl.api.conversation.domain.ConversationRepository;
import io.mopl.api.conversation.domain.DirectMessage;
import io.mopl.api.conversation.domain.DirectMessageRepository;
import io.mopl.api.conversation.dto.ConversationDto;
import io.mopl.api.conversation.dto.DirectMessageDto;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.dto.UserSummary;
import io.mopl.api.user.mapper.UserMapper;
import io.mopl.core.error.BusinessException;
import java.time.Instant;
import java.util.List;
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
  private final UserMapper userMapper;

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

    UserSummary with = userMapper.toSummary(withUser, thumbnailUrl);

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

  @Transactional(readOnly = true)
  public ConversationDto findById(UUID conversationId, UUID userId) {
    Conversation conversation =
        conversationRepository
            .findById(conversationId)
            .orElseThrow(
                () -> {
                  log.info("대화 조회 실패: 대화를 찾을 수 없음. conversationId={}", conversationId);
                  return new BusinessException(ConversationErrorCode.CONVERSATION_NOT_FOUND)
                      .addDetail("conversationId", conversationId.toString());
                });

    List<ConversationParticipant> participants =
        conversationParticipantRepository.findAllByConversationId(conversationId);
    ConversationParticipant me =
        participants.stream()
            .filter(cp -> cp.getId().getUserId().equals(userId))
            .findFirst()
            .orElseThrow(
                () -> {
                  log.info(
                      "대화 참여자를 찾을 수 없습니다: conversationId={}, userId={}", conversationId, userId);
                  return new BusinessException(
                          ConversationErrorCode.CONVERSATION_PARTICIPANT_NOT_FOUND)
                      .addDetail("conversationId", conversationId.toString())
                      .addDetail("userId", userId.toString());
                });

    UUID withUserId =
        participants.stream()
            .filter(cp -> !cp.getId().getUserId().equals(userId))
            .map(cp -> cp.getId().getUserId())
            .findFirst()
            .orElseThrow(
                () -> {
                  log.info(
                      "대화 상대를 찾을 수 없습니다: conversationId={}, userId={}", conversationId, userId);
                  return new BusinessException(
                          ConversationErrorCode.CONVERSATION_PARTICIPANT_NOT_FOUND)
                      .addDetail("conversationId", conversationId.toString())
                      .addDetail("userId", userId.toString());
                });

    User withUser =
        userRepository
            .findById(withUserId)
            .orElseThrow(
                () -> {
                  log.info("사용자를 찾을 수 없습니다: id={}", withUserId);
                  return new BusinessException(UserErrorCode.USER_NOT_FOUND)
                      .addDetail("withUserId", withUserId.toString());
                });

    String thumbnailUrl =
        contentThumbnailUploadService.generatePresignedUrl(withUser.getProfileImageUrl());

    UserSummary with = userMapper.toSummary(withUser, thumbnailUrl);

    DirectMessage lastestMessage =
        directMessageRepository.findLatestByConversationId(conversationId).orElse(null);

    Instant lastReadAt = me.getLastReadAt();

    DirectMessageDto lastestMessageDto = null;
    boolean hasUnread = false;
    if (lastestMessage != null) {
      User currentUser =
          userRepository
              .findById(userId)
              .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

      User sender;
      User receiver;

      if (lastestMessage.getSenderId().equals(userId)) {
        sender = currentUser;
        receiver = withUser;
      } else {
        sender = withUser;
        receiver = currentUser;
      }

      String senderProfileImageUrl =
          contentThumbnailUploadService.generatePresignedUrl(sender.getProfileImageUrl());
      String receiverProfileImageUrl =
          contentThumbnailUploadService.generatePresignedUrl(receiver.getProfileImageUrl());
      UserSummary senderSummary = userMapper.toSummary(sender, senderProfileImageUrl);
      UserSummary receiverSummary = userMapper.toSummary(receiver, receiverProfileImageUrl);
      lastestMessageDto =
          DirectMessageDto.builder()
              .id(lastestMessage.getId())
              .conversationId(lastestMessage.getConversationId())
              .createdAt(lastestMessage.getCreatedAt())
              .sender(senderSummary)
              .receiver(receiverSummary)
              .content(lastestMessage.getContent())
              .build();
      Instant lastReadAtOrEpoch = lastReadAt == null ? Instant.EPOCH : lastReadAt;
      hasUnread = lastReadAtOrEpoch.isBefore(lastestMessage.getCreatedAt());
    }

    return ConversationDto.builder()
        .id(conversation.getId())
        .with(with)
        .lastestMessage(lastestMessageDto)
        .hasUnread(hasUnread)
        .build();
  }
}
