package io.mopl.api.content.domain;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.mopl.api.content.dto.ContentPage;
import io.mopl.api.content.dto.ContentSearchRequest;
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
public class ContentQueryRepositoryImpl implements ContentQueryRepository {
  private static final QContent c = QContent.content;
  QContentTag ct = QContentTag.contentTag;
  QTag t = QTag.tag;
  private final JPAQueryFactory queryFactory;

  @Override
  public ContentPage findContentPage(ContentSearchRequest request) {
    SortBy sortBy = SortBy.from(request.getSortByOrDefault());
    SortDirection sortDirection = SortDirection.from(request.getSortDirectionOrDefault());

    BooleanBuilder where =
        buildBaseWhere(request.getTypeEqual(), request.getKeywordLike(), request.getTagsIn());

    if (request.getCursor() != null && !request.getCursor().isBlank()) {
      if (request.getIdAfter() == null) {
        throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
            .addDetail("reason", "cursor가 있으면 idAfter도 필수이다.");
      }
      BooleanExpression cursorCondition =
          buildCursorCondition(request.getCursor(), request.getIdAfter(), sortBy, sortDirection);
      where.and(cursorCondition);
    }

    OrderSpecifier<?> primaryOrder = buildPrimaryOrder(sortBy, sortDirection);

    List<Content> fetched =
        queryFactory
            .selectFrom(c)
            .where(where)
            .orderBy(primaryOrder, (sortDirection == SortDirection.DESC) ? c.id.desc() : c.id.asc())
            .limit(request.getLimitOrDefault() + 1)
            .fetch();

    boolean hasNext = fetched.size() > request.getLimitOrDefault();
    if (hasNext) {
      fetched = fetched.subList(0, request.getLimitOrDefault());
    }

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (hasNext && !fetched.isEmpty()) {
      Content last = fetched.get(fetched.size() - 1);

      nextIdAfter = last.getId();
      if (sortBy == SortBy.CREATED_AT) {
        nextCursor = String.valueOf(last.getCreatedAt());
      } else if (sortBy == SortBy.RATE) {
        nextCursor = String.valueOf(last.getAverageRating());
      } else {
        nextCursor = String.valueOf(last.getWatcherCount());
      }
    }

    return new ContentPage(fetched, hasNext, nextCursor, nextIdAfter);
  }

  @Override
  public long countContents(String typeEqual, String keywordLike, List<String> tagsIn) {

    BooleanBuilder where = buildBaseWhere(typeEqual, keywordLike, tagsIn);

    Long count = queryFactory.select(c.count()).from(c).where(where).fetchOne();

    return count != null ? count : 0L;
  }

  private OrderSpecifier<?> buildPrimaryOrder(SortBy sortBy, SortDirection sortDirection) {
    if (sortBy == SortBy.CREATED_AT) {
      return (sortDirection == SortDirection.DESC) ? c.createdAt.desc() : c.createdAt.asc();
    }
    if (sortBy == SortBy.RATE) {
      return (sortDirection == SortDirection.DESC) ? c.averageRating.desc() : c.averageRating.asc();
    }
    return (sortDirection == SortDirection.DESC) ? c.watcherCount.desc() : c.watcherCount.asc();
  }

  private BooleanExpression buildCursorCondition(
      String cursor, UUID idAfter, SortBy sortBy, SortDirection sortDirection) {
    if (sortBy == SortBy.CREATED_AT) {
      Instant time;
      try {
        time = Instant.parse(cursor);
      } catch (DateTimeParseException e) {
        throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
            .addDetail("reason", "잘못된 cursor 형식입니다.")
            .addDetail("cursor", cursor);
      }
      if (sortDirection == SortDirection.DESC) {
        return c.createdAt.lt(time).or(c.createdAt.eq(time).and(c.id.lt(idAfter)));
      }
      return c.createdAt.gt(time).or(c.createdAt.eq(time).and(c.id.gt(idAfter)));
    }
    if (sortBy == SortBy.RATE) {
      double rate;
      try {
        rate = Double.parseDouble(cursor);
      } catch (NumberFormatException e) {
        throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
            .addDetail("reason", "잘못된 cursor 형식입니다.")
            .addDetail("cursor", cursor);
      }

      if (sortDirection == SortDirection.DESC) {
        return c.averageRating.lt(rate).or(c.averageRating.eq(rate).and(c.id.lt(idAfter)));
      }
      return c.averageRating.gt(rate).or(c.averageRating.eq(rate).and(c.id.gt(idAfter)));
    }

    long wc;
    try {
      wc = Long.parseLong(cursor);
    } catch (NumberFormatException e) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
          .addDetail("reason", "잘못된 cursor 형식입니다.")
          .addDetail("cursor", cursor);
    }
    if (sortDirection == SortDirection.DESC) {
      return c.watcherCount.lt(wc).or(c.watcherCount.eq(wc).and(c.id.lt(idAfter)));
    }
    return c.watcherCount.gt(wc).or(c.watcherCount.eq(wc).and(c.id.gt(idAfter)));
  }

  private BooleanBuilder buildBaseWhere(String typeEqual, String keywordLike, List<String> tagsIn) {
    BooleanBuilder where = new BooleanBuilder();

    if (typeEqual != null && !typeEqual.isBlank()) {
      switch (typeEqual) {
        case "movie" -> where.and(c.type.eq(ContentType.MOVIE));
        case "tvSeries" -> where.and(c.type.eq(ContentType.TV_SERIES));
        case "sport" -> where.and(c.type.eq(ContentType.SPORT));
      }
    }
    if (keywordLike != null && !keywordLike.isBlank()) {
      var descriptionAsString = Expressions.stringTemplate("cast({0} as char)", c.description);

      where.and(
          c.title
              .containsIgnoreCase(keywordLike)
              .or(descriptionAsString.containsIgnoreCase(keywordLike)));
    }
    if (tagsIn != null && !tagsIn.isEmpty()) {
      int tagCount = (int) tagsIn.stream().distinct().count();

      where.and(
          c.id.in(
              JPAExpressions.select(ct.id.contentId)
                  .from(ct)
                  .join(t)
                  .on(ct.id.tagId.eq(t.id))
                  .where(t.name.in(tagsIn))
                  .groupBy(ct.id.contentId)
                  .having(t.name.countDistinct().eq((long) tagCount))));
    }

    return where;
  }

  private enum SortBy {
    CREATED_AT,
    WATCHER_COUNT,
    RATE;

    static SortBy from(String from) {
      return switch (from) {
        case "createdAt" -> CREATED_AT;
        case "rate" -> RATE;
        default -> WATCHER_COUNT;
      };
    }
  }

  private enum SortDirection {
    ASC,
    DESC;

    static SortDirection from(String direction) {
      return switch (direction) {
        case "ASCENDING" -> ASC;
        default -> DESC;
      };
    }
  }
}
