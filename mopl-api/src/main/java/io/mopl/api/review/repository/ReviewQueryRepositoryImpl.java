package io.mopl.api.review.repository;

import static io.mopl.api.review.domain.QReview.review;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.mopl.api.common.dto.CursorResponse;
import io.mopl.api.common.dto.SortDirection;
import io.mopl.api.review.domain.Review;
import io.mopl.api.review.dto.ReviewCursorRequest;
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
public class ReviewQueryRepositoryImpl implements ReviewQueryRepository {
  private static final int MIN_LIMIT = 1;
  private static final int MAX_LIMIT = 100;

  private final JPAQueryFactory queryFactory;

  @Override
  public CursorResponse<Review> findReviewsPage(UUID contentId, ReviewCursorRequest cursorRequest) {

    int limit = cursorRequest.getLimitOrDefault();
    if (limit < MIN_LIMIT || limit > MAX_LIMIT) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
          .addDetail("limit", String.valueOf(limit));
    }
    String sortBy = normalizeSortBy(cursorRequest.getSortBy());
    String sortDirection = normalizeSortDirection(cursorRequest.getSortDirection());
    String cursor = cursorRequest.getCursor();
    UUID idAfter = cursorRequest.getIdAfter();

    // 1. 데이터 조회
    List<Review> reviews =
        queryFactory
            .selectFrom(review)
            .where(
                review.contentId.eq(contentId),
                cursorCondition(cursor, idAfter, sortBy, sortDirection))
            .orderBy(getOrderSpecifier(sortBy, sortDirection))
            .limit(limit + 1) // 다음 페이지 존재 확인
            .fetch(); // 쿼리를 실제 db로 날리는 실행 버튼

    // 2. hasNext 확인 및 데이터 슬라이싱
    boolean hasNext = reviews.size() > limit;
    if (hasNext) {
      reviews.remove(limit);
    }

    // 3. 다음 커서 생성
    String nextCursor = null;
    UUID nextIdAfter = null;
    if (!reviews.isEmpty()) { // 리뷰 데이터 있을 경우
      Review lastReview = reviews.get(reviews.size() - 1); // 마지막 리뷰
      nextIdAfter = lastReview.getId();

      if ("rating".equalsIgnoreCase(sortBy)) { // rating = 별점순
        nextCursor = String.valueOf(lastReview.getRating()); // 마지막 리뷰, 별점순으로 저장
      } else {
        nextCursor = lastReview.getCreatedAt().toString(); // 정렬 기준 != 별점순, 최신순으로 실행
      }
    }

    // 4. 전체 카운트 조회
    long totalCount = countReviews(contentId);

    // 5. 응답 객체 빌드
    // String보다 미리 정의된 ENUM 쓰는 게 더 안전
    SortDirection directionEnum =
        "ASCENDING".equalsIgnoreCase(sortDirection) // 하드코딩한 이유 + enum
            ? SortDirection.ASCENDING
            : SortDirection.DESCENDING;

    // 응답 객체에 담아 클라이언트에게 전달 + 빌더 패턴 사용
    return CursorResponse.<Review>builder()
        .data(reviews)
        .nextCursor(nextCursor)
        .nextIdAfter(nextIdAfter)
        .hasNext(hasNext)
        .totalCount(totalCount)
        .sortBy(sortBy)
        .sortDirection(directionEnum)
        .build();
  }

  @Override
  public long countReviews(UUID contentId) {
    Long count =
        queryFactory
            .select(review.count())
            .from(review)
            .where(review.contentId.eq(contentId))
            .fetchOne();
    return count != null ? count : 0;
  }

  private OrderSpecifier<?>[] getOrderSpecifier(String sortBy, String sortDirection) {
    Order order = "ASCENDING".equals(sortDirection) ? Order.ASC : Order.DESC;

    if ("rating".equalsIgnoreCase(sortBy)) {
      return new OrderSpecifier[] {
        new OrderSpecifier<>(order, review.rating), new OrderSpecifier<>(Order.ASC, review.id)
      };

    } else {
      return new OrderSpecifier[] {
        new OrderSpecifier<>(order, review.createdAt), new OrderSpecifier<>(Order.ASC, review.id)
      };
    }
  }

  private BooleanExpression cursorCondition(
      String cursor, UUID idAfter, String sortBy, String sortDirection) {

    boolean hasCursor = cursor != null && !cursor.isBlank();
    if (hasCursor ^ (idAfter != null)) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
          .addDetail("reason", "cursor requires idAfter")
          .addDetail("cursor", String.valueOf(cursor))
          .addDetail("idAfter", String.valueOf(idAfter));
    }
    if (!hasCursor) {
      return null;
    }

    String trimmedCursor = cursor.trim();
    boolean isAscending = "ASCENDING".equals(sortDirection);
    if ("rating".equalsIgnoreCase(sortBy)) {
      double ratingCursor;
      try {
        ratingCursor = Double.parseDouble(trimmedCursor);
      } catch (NumberFormatException e) {
        throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
            .addDetail("reason", "invalid cursor format")
            .addDetail("cursor", trimmedCursor);
      }
      return isAscending
          // 오름차순: (평점 > 커서) OR (평점 == 커서 AND ID > 커서ID)
          ? review
              .rating
              .gt(ratingCursor)
              .or(review.rating.eq(ratingCursor).and(review.id.gt(idAfter)))
          // 내림차순: (평점 < 커서) OR (평점 == 커서 AND ID > 커서ID)
          : review
              .rating
              .lt(ratingCursor)
              .or(review.rating.eq(ratingCursor).and(review.id.gt(idAfter)));
    } else {
      Instant createdAtCursor;
      try {
        createdAtCursor = Instant.parse(trimmedCursor);
      } catch (DateTimeParseException e) {
        throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
            .addDetail("reason", "invalid cursor format")
            .addDetail("cursor", trimmedCursor);
      }

      return isAscending
          // 오름차순: (작성일 > 커서) OR (작성일 == 커서 AND ID > 커서ID)
          ? review
              .createdAt
              .gt(createdAtCursor)
              .or(review.createdAt.eq(createdAtCursor).and(review.id.gt(idAfter)))
          // 내림차순: (작성일 < 커서) OR (작성일 == 커서 AND ID > 커서ID)
          : review
              .createdAt
              .lt(createdAtCursor)
              .or(review.createdAt.eq(createdAtCursor).and(review.id.gt(idAfter)));
    }
  }

  private String normalizeSortBy(String sortBy) {
    if (sortBy == null || sortBy.isBlank()) {
      return "createdAt";
    }
    if ("rating".equalsIgnoreCase(sortBy)) {
      return "rating";
    }
    if ("createdAt".equalsIgnoreCase(sortBy)) {
      return "createdAt";
    }
    throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
        .addDetail("sortBy", String.valueOf(sortBy));
  }

  private String normalizeSortDirection(String sortDirection) {
    if (sortDirection == null || sortDirection.isBlank()) {
      return "DESCENDING";
    }
    if ("ASCENDING".equalsIgnoreCase(sortDirection)) {
      return "ASCENDING";
    }
    if ("DESCENDING".equalsIgnoreCase(sortDirection)) {
      return "DESCENDING";
    }
    throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
        .addDetail("sortDirection", String.valueOf(sortDirection));
  }
}
