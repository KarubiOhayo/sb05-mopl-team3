package io.mopl.api.playlist.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.mopl.api.playlist.domain.Playlist;
import io.mopl.api.playlist.domain.QPlaylist;
import io.mopl.api.playlist.domain.QPlaylistSubscription;
import io.mopl.api.playlist.dto.PlaylistPage;
import io.mopl.api.playlist.dto.PlaylistSearchRequest;
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
public class PlaylistQueryRepositoryImpl implements PlaylistQueryRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public PlaylistPage findPlaylistsPage(PlaylistSearchRequest request) {
    // 기본 페이징/정렬 값
    int limit = request.getLimitOrDefault();
    String sortByRaw = request.getSortByOrDefault();
    String sortDirectionRaw = request.getSortDirectionOrDefault();

    // 내부 enum 변환
    SortBy sortBy = SortBy.from(sortByRaw);
    SortDirection sortDirection = SortDirection.from(sortDirectionRaw);

    QPlaylist p = QPlaylist.playlist;

    // 기본 필터 구성
    BooleanBuilder where =
        buildBaseWhere(
            request.getKeywordLike(), request.getOwnerIdEqual(), request.getSubscriberIdEqual(), p);

    // 커서 조건 (cursor + idAfter)
    String cursor = request.getCursor();
    UUID idAfter = request.getIdAfter();
    if (cursor != null && !cursor.isBlank()) {
      if (idAfter == null) {
        throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
            .addDetail("reason", "cursor requires idAfter");
      }
      BooleanExpression cursorCondition =
          buildCursorCondition(cursor, idAfter, sortBy, sortDirection, p);
      where.and(cursorCondition);
    }

    // 정렬 + limit+1 조회로 다음 페이지 여부 판단
    OrderSpecifier<?> primaryOrder = buildPrimaryOrder(sortBy, sortDirection, p);
    List<Playlist> fetched =
        queryFactory
            .selectFrom(p)
            .where(where)
            .orderBy(primaryOrder, (sortDirection == SortDirection.DESC) ? p.id.desc() : p.id.asc())
            .limit(limit + 1)
            .fetch();

    boolean hasNext = fetched.size() > limit;
    if (hasNext) {
      fetched = fetched.subList(0, limit);
    }

    // nextCursor/nextIdAfter 계산
    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !fetched.isEmpty()) {
      Playlist last = fetched.get(fetched.size() - 1);
      nextIdAfter = last.getId();
      nextCursor =
          (sortBy == SortBy.UPDATED_AT)
              ? String.valueOf(last.getUpdatedAt())
              : String.valueOf(last.getSubscriberCount());
    }

    return new PlaylistPage(fetched, hasNext, nextCursor, nextIdAfter);
  }

  @Override
  public long countPlaylists(PlaylistSearchRequest request) {
    QPlaylist p = QPlaylist.playlist;

    // 목록 조회와 동일한 필터로 count 계산
    BooleanBuilder where =
        buildBaseWhere(
            request.getKeywordLike(), request.getOwnerIdEqual(), request.getSubscriberIdEqual(), p);

    Long count = queryFactory.select(p.count()).from(p).where(where).fetchOne();

    return count != null ? count.longValue() : 0L;
  }

  // 기본 필터 구성 shared by list/count.
  private BooleanBuilder buildBaseWhere(
      String keywordLike, UUID ownerIdEqual, UUID subscriberIdEqual, QPlaylist p) {
    BooleanBuilder where = new BooleanBuilder();

    // 제목 검색 (대소문자 무시)
    if (keywordLike != null && !keywordLike.isBlank()) {
      where.and(p.title.containsIgnoreCase(keywordLike));
    }

    // 소유자 필터
    if (ownerIdEqual != null) {
      where.and(p.ownerId.eq(ownerIdEqual));
    }

    // 구독자 필터 (exists 서브쿼리)
    if (subscriberIdEqual != null) {
      QPlaylistSubscription s = QPlaylistSubscription.playlistSubscription;

      where.and(
          JPAExpressions.selectOne()
              .from(s)
              .where(s.id.playlistId.eq(p.id).and(s.id.userId.eq(subscriberIdEqual)))
              .exists());
    }

    return where;
  }

  // 커서 페이지네이션 조건 (id tie-breaker 포함)
  private BooleanExpression buildCursorCondition(
      String cursor, UUID idAfter, SortBy sortBy, SortDirection sortDirection, QPlaylist p) {
    if (sortBy == SortBy.UPDATED_AT) {
      Instant c;
      try {
        c = Instant.parse(cursor);
      } catch (DateTimeParseException e) {
        throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
            .addDetail("reason", "invalid cursor format")
            .addDetail("cursor", cursor);
      }

      if (sortDirection == SortDirection.DESC) {
        return p.updatedAt.lt(c).or(p.updatedAt.eq(c).and(p.id.lt(idAfter)));
      }
      return p.updatedAt.gt(c).or(p.updatedAt.eq(c).and(p.id.gt(idAfter)));
    }

    // sortBy == SUBSCRIBE_COUNT
    long c;
    try {
      c = Long.parseLong(cursor);
    } catch (NumberFormatException e) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
          .addDetail("reason", "invalid cursor format")
          .addDetail("cursor", cursor);
    }

    if (sortDirection == SortDirection.DESC) {
      return p.subscriberCount.lt(c).or(p.subscriberCount.eq(c).and(p.id.lt(idAfter)));
    }
    return p.subscriberCount.gt(c).or(p.subscriberCount.eq(c).and(p.id.gt(idAfter)));
  }

  // 기본 정렬 컬럼
  private OrderSpecifier<?> buildPrimaryOrder(
      SortBy sortBy, SortDirection sortDirection, QPlaylist p) {
    if (sortBy == SortBy.UPDATED_AT) {
      return (sortDirection == SortDirection.DESC) ? p.updatedAt.desc() : p.updatedAt.asc();
    }
    return (sortDirection == SortDirection.DESC)
        ? p.subscriberCount.desc()
        : p.subscriberCount.asc();
  }

  // sortBy 문자열 파싱
  private enum SortBy {
    UPDATED_AT,
    SUBSCRIBE_COUNT;

    static SortBy from(String raw) {
      if (raw == null || raw.isBlank()) {
        return UPDATED_AT;
      }
      if ("updatedAt".equals(raw)) {
        return UPDATED_AT;
      }
      if ("subscribeCount".equals(raw)) {
        return SUBSCRIBE_COUNT;
      }
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
          .addDetail("sortBy", String.valueOf(raw));
    }
  }

  // sortDirection 문자열 파싱
  private enum SortDirection {
    ASC,
    DESC;

    static SortDirection from(String raw) {
      if (raw == null || raw.isBlank()) {
        return DESC;
      }
      if ("ASCENDING".equals(raw)) {
        return ASC;
      }
      if ("DESCENDING".equals(raw)) {
        return DESC;
      }
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
          .addDetail("sortDirection", String.valueOf(raw));
    }
  }
}
