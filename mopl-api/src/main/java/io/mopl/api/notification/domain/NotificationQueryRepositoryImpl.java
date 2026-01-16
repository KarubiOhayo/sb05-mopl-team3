package io.mopl.api.notification.domain;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.mopl.api.common.dto.SortDirection;
import io.mopl.api.notification.dto.NotificationPage;
import io.mopl.api.notification.dto.NotificationSearchRequest;
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
public class NotificationQueryRepositoryImpl implements NotificationQueryRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public NotificationPage findUnreadPage(UUID receiverId, NotificationSearchRequest request) {
    QNotification n = QNotification.notification;

    int limit = request.limit();
    SortDirection sortDirection = request.sortDirection();

    // 미읽음 알림만 조회
    BooleanBuilder where =
        new BooleanBuilder().and(n.receiverId.eq(receiverId)).and(n.readAt.isNull());

    String cursor = request.cursor();
    UUID idAfter = request.idAfter();
    if (cursor != null && !cursor.isBlank()) {
      if (idAfter == null) {
        throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
            .addDetail("reason", "cursor requires idAfter");
      }
      where.and(buildCursorCondition(cursor, idAfter, sortDirection, n));
    }

    OrderSpecifier<?> primaryOrder = buildPrimaryOrder(sortDirection, n);
    List<Notification> fetched =
        queryFactory
            .selectFrom(n)
            .where(where)
            .orderBy(
                primaryOrder, sortDirection == SortDirection.DESCENDING ? n.id.desc() : n.id.asc())
            .limit(limit + 1)
            .fetch();

    boolean hasNext = fetched.size() > limit;
    if (hasNext) {
      fetched = fetched.subList(0, limit);
    }

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !fetched.isEmpty()) {
      Notification last = fetched.get(fetched.size() - 1);
      nextCursor = String.valueOf(last.getCreatedAt());
      nextIdAfter = last.getId();
    }

    return new NotificationPage(fetched, hasNext, nextCursor, nextIdAfter);
  }

  @Override
  public long countUnread(UUID receiverId) {
    QNotification n = QNotification.notification;
    Long count =
        queryFactory
            .select(n.count())
            .from(n)
            .where(n.receiverId.eq(receiverId).and(n.readAt.isNull()))
            .fetchOne();
    return count != null ? count.longValue() : 0L;
  }

  // createdAt + id 기준 커서 페이징을 적용
  private BooleanExpression buildCursorCondition(
      String cursor, UUID idAfter, SortDirection sortDirection, QNotification n) {
    Instant createdAt;
    try {
      createdAt = Instant.parse(cursor);
    } catch (DateTimeParseException e) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
          .addDetail("reason", "invalid cursor format")
          .addDetail("cursor", cursor);
    }

    if (sortDirection == SortDirection.DESCENDING) {
      return n.createdAt.lt(createdAt).or(n.createdAt.eq(createdAt).and(n.id.lt(idAfter)));
    }
    return n.createdAt.gt(createdAt).or(n.createdAt.eq(createdAt).and(n.id.gt(idAfter)));
  }

  private OrderSpecifier<?> buildPrimaryOrder(SortDirection sortDirection, QNotification n) {
    return sortDirection == SortDirection.DESCENDING ? n.createdAt.desc() : n.createdAt.asc();
  }
}
