package io.mopl.api.conversation.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.mopl.api.common.dto.SortDirection;
import io.mopl.api.conversation.domain.Conversation;
import io.mopl.api.conversation.domain.QConversation;
import io.mopl.api.conversation.domain.QConversationParticipant;
import io.mopl.api.conversation.domain.QDirectMessage;
import io.mopl.api.conversation.dto.ConversationPage;
import io.mopl.api.conversation.dto.ConversationSearchRequest;
import io.mopl.api.user.domain.QUser;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ConversationQueryRepositoryImpl implements ConversationQueryRepository {
  private static final QConversation c = QConversation.conversation;
  private static final QConversationParticipant cp =
      QConversationParticipant.conversationParticipant;
  private static final QDirectMessage dm = QDirectMessage.directMessage;
  private final JPAQueryFactory queryFactory;

  @Override
  public ConversationPage findConversationPage(UUID userId, ConversationSearchRequest request) {
    SortDirection sortDirection = request.sortDirection();

    BooleanBuilder where = new BooleanBuilder();
    where.and(cp.id.userId.eq(userId));

    String keywordLike = request.keywordLike();
    if (keywordLike != null && !keywordLike.isBlank()) {
      where.and(buildKeywordPredicate(userId, keywordLike.trim()));
    }

    if (request.cursor() != null && !request.cursor().isBlank()) {
      if (request.idAfter() == null) {
        throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
            .addDetail("reason", "cursor가 있으면 idAfter도 필수입니다.")
            .addDetail("cursor", request.cursor());
      }
      Instant cursorTime;
      try {
        cursorTime = Instant.parse(request.cursor());
      } catch (DateTimeParseException e) {
        throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
            .addDetail("reason", "잘못된 cursor 형식입니다.")
            .addDetail("cursor", request.cursor());
      }
      BooleanExpression cursorCondition;
      if (sortDirection == SortDirection.DESCENDING) {
        cursorCondition =
            c.createdAt
                .lt(cursorTime)
                .or(c.createdAt.eq(cursorTime).and(c.id.lt(request.idAfter())));
      } else {
        cursorCondition =
            c.createdAt
                .gt(cursorTime)
                .or(c.createdAt.eq(cursorTime).and(c.id.gt(request.idAfter())));
      }
      where.and(cursorCondition);
    }

    OrderSpecifier<?> primaryOrder =
        (sortDirection == SortDirection.DESCENDING) ? c.createdAt.desc() : c.createdAt.asc();
    OrderSpecifier<?> secondaryOrder =
        (sortDirection == SortDirection.DESCENDING) ? c.id.desc() : c.id.asc();

    List<Conversation> fetched =
        queryFactory
            .select(c)
            .from(c)
            .join(cp)
            .on(cp.id.conversationId.eq(c.id))
            .where(where)
            .orderBy(primaryOrder, secondaryOrder)
            .limit(request.limit() + 1L)
            .fetch();

    boolean hasNext = fetched.size() > request.limit();
    if (hasNext) {
      fetched = fetched.subList(0, request.limit());
    }

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !fetched.isEmpty()) {
      Conversation last = fetched.getLast();
      nextCursor = String.valueOf(last.getCreatedAt());
      nextIdAfter = last.getId();
    }

    return new ConversationPage(fetched, hasNext, nextCursor, nextIdAfter);
  }

  @Override
  public long countConversations(UUID userId, String keywordLike) {
    BooleanBuilder where = new BooleanBuilder();
    where.and(cp.id.userId.eq(userId));

    if (keywordLike != null && !keywordLike.isBlank()) {
      where.and(buildKeywordPredicate(userId, keywordLike.trim()));
    }

    Long count =
        queryFactory
            .select(c.count())
            .from(c)
            .join(cp)
            .on(cp.id.conversationId.eq(c.id))
            .where(where)
            .fetchOne();

    return count != null ? count : 0L;
  }

  private BooleanExpression buildKeywordPredicate(UUID userId, String keywordLike) {
    QConversationParticipant cpOther = new QConversationParticipant("cpOther");
    QUser u = new QUser("u");

    BooleanExpression nameMatch =
        JPAExpressions.selectOne()
            .from(cpOther)
            .join(u)
            .on(cpOther.id.userId.eq(u.id))
            .where(
                cpOther
                    .id
                    .conversationId
                    .eq(c.id)
                    .and(cpOther.id.userId.ne(userId))
                    .and(u.name.containsIgnoreCase(keywordLike)))
            .exists();

    BooleanExpression messageMatch =
        JPAExpressions.selectOne()
            .from(dm)
            .where(dm.conversationId.eq(c.id).and(dm.content.containsIgnoreCase(keywordLike)))
            .exists();

    return nameMatch.or(messageMatch);
  }
}
