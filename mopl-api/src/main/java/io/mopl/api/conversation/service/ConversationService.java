package io.mopl.api.conversation.service;

import io.mopl.api.common.dto.CursorResponse;
import io.mopl.api.common.error.ConversationErrorCode;
import io.mopl.api.common.error.UserErrorCode;
import io.mopl.api.content.service.ContentThumbnailUploadService;
import io.mopl.api.conversation.domain.Conversation;
import io.mopl.api.conversation.domain.ConversationParticipant;
import io.mopl.api.conversation.domain.ConversationParticipantId;
import io.mopl.api.conversation.domain.DirectMessage;
import io.mopl.api.conversation.dto.ConversationDto;
import io.mopl.api.conversation.dto.ConversationPage;
import io.mopl.api.conversation.dto.ConversationSearchRequest;
import io.mopl.api.conversation.dto.DirectMessageDto;
import io.mopl.api.conversation.repository.ConversationParticipantRepository;
import io.mopl.api.conversation.repository.ConversationRepository;
import io.mopl.api.conversation.repository.DirectMessageRepository;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.dto.UserSummary;
import io.mopl.api.user.mapper.UserMapper;
import io.mopl.core.error.BusinessException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
      log.warn("대화 | 생성 | 실패: 인증 사용자 ID 없음");
      throw new BusinessException(UserErrorCode.UNAUTHORIZED);
    }
    if (withUserId == null) {
      log.info("대화 | 생성 | 실패: withUserId 누락. userId={}", userId);
      throw new BusinessException(ConversationErrorCode.WITH_USER_ID_REQUIRED);
    }
    if (userId.equals(withUserId)) {
      log.info("대화 | 생성 | 실패: 자기 자신과의 대화 요청. userId={}", userId);
      throw new BusinessException(ConversationErrorCode.SAME_USER_NOT_ALLOWED);
    }

    conversationParticipantRepository
        .findConversationIdByParticipants(userId, withUserId)
        .ifPresent(
            existingConversationId -> {
              log.info(
                  "대화 | 생성 | 실패: 기존 대화 존재. userId={}, withUserId={}, conversationId={}",
                  userId,
                  withUserId,
                  existingConversationId);
              throw new BusinessException(ConversationErrorCode.CONVERSATION_ALREADY_EXISTS)
                  .addDetail("conversationId", existingConversationId.toString());
            });

    log.debug("대화 | 생성 | 시작: userId={}, withUserId={}", userId, withUserId);
    User withUser =
        userRepository
            .findById(withUserId)
            .orElseThrow(
                () ->
                    new BusinessException(UserErrorCode.USER_NOT_FOUND)
                        .addDetail("withUserId", withUserId.toString()));

    String thumbnailUrl =
        contentThumbnailUploadService.generatePresignedUrl(withUser.getProfileImageKey());

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
        "대화 | 생성 | 완료: conversationId={}, userId={}, withUserId={}",
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
    if (userId == null) {
      log.warn("대화 | 단건 조회 | 실패: 인증 사용자 ID 없음. conversationId={}", conversationId);
      throw new BusinessException(UserErrorCode.UNAUTHORIZED);
    }
    log.debug("대화 | 단건 조회 | 시작: conversationId={}, userId={}", conversationId, userId);
    Conversation conversation =
        conversationRepository
            .findById(conversationId)
            .orElseThrow(
                () -> {
                  log.info("대화 | 단건 조회 | 실패: 대화를 찾을 수 없음. conversationId={}", conversationId);
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
                      "대화 | 단건 조회 | 실패: 대화 참여자 없음. conversationId={}, userId={}",
                      conversationId,
                      userId);
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
                      "대화 | 단건 조회 | 실패: 대화 상대 없음. conversationId={}, userId={}",
                      conversationId,
                      userId);
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
                  log.info("대화 | 단건 조회 | 실패: 사용자 없음. id={}", withUserId);
                  return new BusinessException(UserErrorCode.USER_NOT_FOUND)
                      .addDetail("withUserId", withUserId.toString());
                });

    String thumbnailUrl =
        contentThumbnailUploadService.generatePresignedUrl(withUser.getProfileImageKey());

    UserSummary with = userMapper.toSummary(withUser, thumbnailUrl);

    DirectMessage lastestMessage =
        directMessageRepository
            .findFirstByConversationIdOrderByCreatedAtDesc(conversationId)
            .orElse(null);

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
          contentThumbnailUploadService.generatePresignedUrl(sender.getProfileImageKey());
      String receiverProfileImageUrl =
          contentThumbnailUploadService.generatePresignedUrl(receiver.getProfileImageKey());
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

  @Transactional(readOnly = true)
  public ConversationDto findByWithUserId(UUID userId, UUID withUserId) {
    if (userId == null) {
      log.warn("대화 | 상대 기준 조회 | 실패: 인증 사용자 ID 없음. withUserId={}", withUserId);
      throw new BusinessException(UserErrorCode.UNAUTHORIZED);
    }
    log.debug("대화 | 상대 기준 조회 | 시작: userId={}, withUserId={}", userId, withUserId);
    UUID conversationId =
        conversationParticipantRepository
            .findConversationIdByParticipants(userId, withUserId)
            .orElseThrow(
                () ->
                    new BusinessException(ConversationErrorCode.CONVERSATION_NOT_FOUND)
                        .addDetail("withUserId", withUserId.toString())
                        .addDetail("userId", userId.toString()));

    log.debug(
        "대화 | 상대 기준 조회 | 매핑 완료: userId={}, withUserId={}, conversationId={}",
        userId,
        withUserId,
        conversationId);
    return findById(conversationId, userId);
  }

  @Transactional(readOnly = true)
  public CursorResponse<ConversationDto> find(UUID userId, ConversationSearchRequest request) {
    if (userId == null) {
      log.warn("대화 | 목록 조회 | 실패: 인증 사용자 ID 없음");
      throw new BusinessException(UserErrorCode.UNAUTHORIZED);
    }
    log.debug(
        "대화 | 목록 조회 | 시작: userId={}, keywordLike={}, cursor={}, idAfter={}, limit={}, sortBy={}, sortDirection={}",
        userId,
        request.keywordLike(),
        request.cursor(),
        request.idAfter(),
        request.limit(),
        request.sortBy(),
        request.sortDirection());
    User currentUser =
        userRepository
            .findById(userId)
            .orElseThrow(
                () ->
                    new BusinessException(UserErrorCode.USER_NOT_FOUND)
                        .addDetail("userId", userId.toString()));

    ConversationPage page = conversationRepository.findConversationPage(userId, request);
    List<Conversation> conversations = page.conversations();
    if (conversations.isEmpty()) {
      long totalCount = conversationRepository.countConversations(userId, request.keywordLike());
      return CursorResponse.<ConversationDto>builder()
          .data(List.of())
          .nextCursor(null)
          .nextIdAfter(null)
          .hasNext(false)
          .totalCount(totalCount)
          .sortBy(request.sortBy())
          .sortDirection(request.sortDirection())
          .build();
    }

    List<UUID> conversationIds = conversations.stream().map(Conversation::getId).toList();

    List<ConversationParticipant> myParticipants =
        conversationParticipantRepository.findAllByConversationIdInAndUserId(
            conversationIds, userId);
    Map<UUID, Instant> lastReadAtByConversation = new HashMap<>();
    for (ConversationParticipant participant : myParticipants) {
      if (participant.getId() == null) {
        continue;
      }
      lastReadAtByConversation.put(
          participant.getId().getConversationId(), participant.getLastReadAt());
    }

    List<ConversationParticipant> otherParticipants =
        conversationParticipantRepository.findAllByConversationIdInAndUserIdNot(
            conversationIds, userId);
    Map<UUID, UUID> withUserIdByConversation =
        otherParticipants.stream()
            .collect(
                Collectors.toMap(
                    cp -> cp.getId().getConversationId(),
                    cp -> cp.getId().getUserId(),
                    (left, right) -> left));

    List<UUID> withUserIds = withUserIdByConversation.values().stream().distinct().toList();
    Map<UUID, User> withUsersById =
        userRepository.findAllById(withUserIds).stream()
            .collect(Collectors.toMap(User::getId, user -> user));

    Map<UUID, UserSummary> withSummaryByConversation = new HashMap<>();
    for (Map.Entry<UUID, UUID> entry : withUserIdByConversation.entrySet()) {
      UUID conversationId = entry.getKey();
      UUID withUserId = entry.getValue();
      User withUser = withUsersById.get(withUserId);
      if (withUser == null) {
        continue;
      }
      String thumbnailUrl =
          contentThumbnailUploadService.generatePresignedUrl(withUser.getProfileImageKey());
      withSummaryByConversation.put(conversationId, userMapper.toSummary(withUser, thumbnailUrl));
    }

    String myThumbnailUrl =
        contentThumbnailUploadService.generatePresignedUrl(currentUser.getProfileImageKey());
    UserSummary meSummary = userMapper.toSummary(currentUser, myThumbnailUrl);

    List<String> conversationIdStrings = conversationIds.stream().map(UUID::toString).toList();
    List<DirectMessage> latestMessages =
        directMessageRepository.findLatestByConversationIds(conversationIdStrings);
    Map<UUID, DirectMessage> latestMessageByConversation = new HashMap<>();
    for (DirectMessage message : latestMessages) {
      UUID conversationId = message.getConversationId();
      DirectMessage existing = latestMessageByConversation.get(conversationId);
      if (existing == null) {
        latestMessageByConversation.put(conversationId, message);
        continue;
      }
      int createdAtCompare = message.getCreatedAt().compareTo(existing.getCreatedAt());
      if (createdAtCompare > 0) {
        latestMessageByConversation.put(conversationId, message);
      } else if (createdAtCompare == 0) {
        UUID existingId = existing.getId();
        UUID candidateId = message.getId();
        if (candidateId != null && (existingId == null || candidateId.compareTo(existingId) > 0)) {
          latestMessageByConversation.put(conversationId, message);
        }
      }
    }

    List<ConversationDto> data =
        conversations.stream()
            .map(
                conversation -> {
                  UUID conversationId = conversation.getId();
                  UserSummary withSummary = withSummaryByConversation.get(conversationId);
                  DirectMessage latestMessage = latestMessageByConversation.get(conversationId);

                  DirectMessageDto lastestMessageDto = null;
                  boolean hasUnread = false;
                  if (latestMessage != null && withSummary != null) {
                    UserSummary senderSummary;
                    UserSummary receiverSummary;
                    if (latestMessage.getSenderId().equals(userId)) {
                      senderSummary = meSummary;
                      receiverSummary = withSummary;
                    } else {
                      senderSummary = withSummary;
                      receiverSummary = meSummary;
                    }
                    lastestMessageDto =
                        DirectMessageDto.builder()
                            .id(latestMessage.getId())
                            .conversationId(latestMessage.getConversationId())
                            .createdAt(latestMessage.getCreatedAt())
                            .sender(senderSummary)
                            .receiver(receiverSummary)
                            .content(latestMessage.getContent())
                            .build();

                    Instant lastReadAt = lastReadAtByConversation.get(conversationId);
                    Instant lastReadAtOrEpoch = lastReadAt == null ? Instant.EPOCH : lastReadAt;
                    hasUnread = lastReadAtOrEpoch.isBefore(latestMessage.getCreatedAt());
                    if (latestMessage.getSenderId().equals(userId)) {
                      hasUnread = false;
                    }
                  }

                  return ConversationDto.builder()
                      .id(conversationId)
                      .with(withSummary)
                      .lastestMessage(lastestMessageDto)
                      .hasUnread(hasUnread)
                      .build();
                })
            .filter(dto -> dto.with() != null)
            .toList();

    long totalCount = conversationRepository.countConversations(userId, request.keywordLike());
    log.debug(
        "대화 | 목록 조회 | 완료: userId={}, count={}, hasNext={}", userId, data.size(), page.hasNext());
    return CursorResponse.<ConversationDto>builder()
        .data(data)
        .nextCursor(page.nextCursor())
        .nextIdAfter(page.nextIdAfter())
        .hasNext(page.hasNext())
        .totalCount(totalCount)
        .sortBy(request.sortBy())
        .sortDirection(request.sortDirection())
        .build();
  }
}
