package io.mopl.api.conversation.service;

import io.mopl.api.common.dto.CursorResponse;
import io.mopl.api.common.error.ConversationErrorCode;
import io.mopl.api.common.error.UserErrorCode;
import io.mopl.api.conversation.domain.ConversationParticipant;
import io.mopl.api.conversation.domain.ConversationParticipantId;
import io.mopl.api.conversation.domain.DirectMessage;
import io.mopl.api.conversation.dto.DirectMessageDto;
import io.mopl.api.conversation.dto.DirectMessageSearchRequest;
import io.mopl.api.conversation.repository.ConversationParticipantRepository;
import io.mopl.api.conversation.repository.ConversationRepository;
import io.mopl.api.conversation.repository.DirectMessageRepository;
import io.mopl.api.user.domain.User;
import io.mopl.api.user.domain.UserRepository;
import io.mopl.api.user.dto.UserSummary;
import io.mopl.core.error.BusinessException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectMessageService {
  private final ConversationParticipantRepository conversationParticipantRepository;
  private final DirectMessageRepository directMessageRepository;
  private final UserRepository userRepository;
  private final ConversationRepository conversationRepository;

  @Transactional(readOnly = true)
  public CursorResponse<DirectMessageDto> find(
      UUID conversationId, DirectMessageSearchRequest request) {
    if (conversationId == null) {
      log.warn("DM | 목록 조회 | 실패: conversationId 누락");
      throw new BusinessException(ConversationErrorCode.CONVERSATION_NOT_FOUND);
    }

    // Check if conversation exists
    if (!conversationRepository.existsById(conversationId)) {
      log.warn("DM | 목록 조회 | 실패: 대화를 찾을 수 없음. conversationId={}", conversationId);
      throw new BusinessException(ConversationErrorCode.CONVERSATION_NOT_FOUND)
          .addDetail("conversationId", conversationId.toString());
    }

    log.debug(
        "DM | 목록 조회 | 시작: conversationId={}, cursor={}, limit={}",
        conversationId,
        request.cursor(),
        request.limit());

    int limit = request.limit();

    PageRequest pageable = PageRequest.of(0, limit + 1);

    List<DirectMessage> messages;

    if (request.cursor() == null) {
      messages =
          directMessageRepository.findByConversationIdOrderByCreatedAtDescIdDesc(
              conversationId, pageable);
    } else {
      String cursorStr = request.cursor();
      try {
        if (cursorStr.contains("_")) {
          String[] parts = cursorStr.split("_");
          Instant createdAt = Instant.parse(parts[0]);
          UUID id = UUID.fromString(parts[1]);
          messages =
              directMessageRepository.findByConversationIdAndCursorWithId(
                  conversationId, createdAt, id, pageable);
        } else {
          Instant createdAt = Instant.parse(cursorStr);
          messages =
              directMessageRepository.findByConversationIdAndCursor(
                  conversationId, createdAt, pageable);
        }
      } catch (Exception e) {
        log.warn("DM | 목록 조회 | 실패: 커서 파싱 오류. cursor={}", cursorStr, e);
        throw new BusinessException(io.mopl.core.error.CommonErrorCode.INVALID_REQUEST)
            .addDetail("cursor", cursorStr);
      }
    }

    boolean hasNext = messages.size() > limit;

    if (hasNext) {
      messages = messages.subList(0, limit);
    }

    String nextCursor = null;
    if (hasNext && !messages.isEmpty()) {
      DirectMessage lastMsg = messages.getLast();
      nextCursor = lastMsg.getCreatedAt().toString() + "_" + lastMsg.getId().toString();
    }

    Set<UUID> userIds = new HashSet<>();
    messages.forEach(
        msg -> {
          userIds.add(msg.getSenderId());
          userIds.add(msg.getReceiverId());
        });

    Map<UUID, UserSummary> userMap =
        userRepository.findAllById(userIds).stream()
            .collect(
                Collectors.toMap(
                    User::getId,
                    user ->
                        UserSummary.builder()
                            .userId(user.getId())
                            .name(user.getName())
                            .profileImageUrl(user.getProfileImageUrl())
                            .build()));

    List<DirectMessageDto> dtos =
        messages.stream()
            .map(
                msg ->
                    DirectMessageDto.builder()
                        .id(msg.getId())
                        .conversationId(msg.getConversationId())
                        .createdAt(msg.getCreatedAt())
                        .content(msg.getContent())
                        .sender(userMap.get(msg.getSenderId()))
                        .receiver(userMap.get(msg.getReceiverId()))
                        .build())
            .toList();

    log.debug(
        "DM | 목록 조회 | 완료: conversationId={}, count={}, hasNext={}",
        conversationId,
        dtos.size(),
        hasNext);

    return CursorResponse.<DirectMessageDto>builder()
        .data(dtos)
        .hasNext(hasNext)
        .nextCursor(nextCursor)
        .build();
  }

  @Transactional
  public void read(UUID conversationId, UUID directMessageId, UUID userId) {
    if (userId == null) {
      log.warn("DM | 읽음 처리 | 실패: 인증 사용자 ID 없음");
      throw new BusinessException(UserErrorCode.UNAUTHORIZED);
    }

    ConversationParticipantId conversationParticipantId =
        new ConversationParticipantId(conversationId, userId);
    ConversationParticipant conversationParticipant =
        conversationParticipantRepository
            .findById(conversationParticipantId)
            .orElseThrow(
                () -> {
                  log.warn(
                      "DM | 읽음 처리 | 실패: 대화 참여자 아님. conversationId={}, userId={}",
                      conversationId,
                      userId);
                  return new BusinessException(
                          ConversationErrorCode.CONVERSATION_PARTICIPANT_NOT_FOUND)
                      .addDetail("userId", userId.toString())
                      .addDetail("conversationId", conversationId.toString());
                });

    DirectMessage message = directMessageRepository.findById(directMessageId).orElse(null);

    if (message == null) {
      log.warn("DM | 읽음 처리 | 실패: 메시지 없음. directMessageId={}", directMessageId);
      // 메시지가 없으면 그냥 조용히 리턴하거나 에러를 던질 수 있음. 여기선 로직상 리턴이었음.
      return;
    }

    if (message.getSenderId().equals(userId)) {
      // 내 메시지는 읽음 처리 대상 아님
      return;
    }

    Instant lastReadAt = conversationParticipant.getLastReadAt();
    if (lastReadAt == null || message.getCreatedAt().isAfter(lastReadAt)) {
      conversationParticipant.setLastReadAt(message.getCreatedAt());
      conversationParticipantRepository.save(conversationParticipant);
      log.debug(
          "DM | 읽음 처리 | 완료: userId={}, conversationId={}, lastReadAt={}",
          userId,
          conversationId,
          message.getCreatedAt());
    }
  }
}
