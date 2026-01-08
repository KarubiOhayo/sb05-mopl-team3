package io.mopl.api.user.domain;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparablePath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.lettuce.core.search.arguments.AggregateArgs.SortDirection;
import io.mopl.api.user.dto.UserPage;
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
public class UserRepositoryImpl implements UserRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public UserPage findUsersPage(
      String emailLike,
      String roleEqual,
      Boolean isLocked,
      String cursor,
      UUID idAfter,
      int limit,
      String sortDirectionRaw,
      String sortByRaw) {

    SortBy sortBy = SortBy.from(sortByRaw);
    SortDirection sortDirection = parseSortDirection(sortDirectionRaw);

    QUser u = QUser.user;

    BooleanBuilder where = buildBaseWhere(emailLike, roleEqual, isLocked, u);

    if (cursor != null && !cursor.isBlank()) {
      if (idAfter == null) {
        throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
            .addDetail("reason", "cursor가 있으면 idAfter도 필수입니다.");
      }
      BooleanExpression cursorCondition =
          buildCursorCondition(cursor, idAfter, sortBy, sortDirection, u);
      where.and(cursorCondition);
    }

    OrderSpecifier<?> primaryOrder = buildPrimaryOrder(sortBy, sortDirection, u);

    List<User> fetched =
        queryFactory
            .selectFrom(u)
            .where(where)
            .orderBy(primaryOrder, (sortDirection == SortDirection.DESC) ? u.id.desc() : u.id.asc())
            .limit(limit + 1)
            .fetch();

    boolean hasNext = fetched.size() > limit;
    if (hasNext) {
      fetched = fetched.subList(0, limit);
    }

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (hasNext && !fetched.isEmpty()) {
      User last = fetched.getLast();
      nextIdAfter = last.getId();

      switch (sortBy) {
        case NAME -> nextCursor = last.getName();
        case EMAIL -> nextCursor = last.getEmail();
        case CREATED_AT -> nextCursor = String.valueOf(last.getCreatedAt());
        case IS_LOCKED -> nextCursor = String.valueOf(last.isLocked());
        case ROLE -> nextCursor = last.getRole().name();
      }
    }

    return new UserPage(fetched, hasNext, nextCursor, nextIdAfter);
  }

  @Override
  public long countUsers(String emailLike, String roleEqual, Boolean isLocked) {
    QUser u = QUser.user;
    BooleanBuilder where = buildBaseWhere(emailLike, roleEqual, isLocked, u);

    Long count = queryFactory.select(u.count()).from(u).where(where).fetchOne();

    return count != null ? count : 0L;
  }

  // ===== Private Helper 메서드 =====

  /** 기본 필터 조건 구성 */
  private BooleanBuilder buildBaseWhere(
      String emailLike, String roleEqual, Boolean isLocked, QUser u) {
    BooleanBuilder where = new BooleanBuilder();

    if (emailLike != null && !emailLike.isBlank()) {
      where.and(u.email.containsIgnoreCase(emailLike).or(u.name.containsIgnoreCase(emailLike)));
    }

    if (roleEqual != null && !roleEqual.isBlank()) {
      try {
        UserRole role = UserRole.valueOf(roleEqual);
        where.and(u.role.eq(role));
      } catch (IllegalArgumentException e) {
        throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
            .addDetail("reason", "잘못된 role 값입니다")
            .addDetail("role", roleEqual);
      }
    }

    if (isLocked != null) {
      where.and(u.locked.eq(isLocked));
    }

    return where;
  }

  /** 커서 페이징 조건 구성 */
  private BooleanExpression buildCursorCondition(
      String cursor, UUID idAfter, SortBy sortBy, SortDirection sortDirection, QUser u) {
    return switch (sortBy) {
      case NAME -> buildStringCursorCondition(cursor, idAfter, u.name, u.id, sortDirection);
      case EMAIL -> buildStringCursorCondition(cursor, idAfter, u.email, u.id, sortDirection);
      case CREATED_AT ->
          buildInstantCursorCondition(cursor, idAfter, u.createdAt, u.id, sortDirection);
      case IS_LOCKED -> buildBooleanCursorCondition(cursor, idAfter, u.locked, u.id, sortDirection);
      case ROLE -> buildRoleCursorCondition(cursor, idAfter, u.role, u.id, sortDirection);
    };
  }

  /** 문자열 필드 커서 조건 */
  private BooleanExpression buildStringCursorCondition(
      String cursor,
      UUID idAfter,
      StringPath field,
      ComparablePath<UUID> idField,
      SortDirection sortDirection) {

    // gt = greater than, lt = less then, eq = equal
    if (sortDirection == SortDirection.DESC) {
      return field.lt(cursor).or(field.eq(cursor).and(idField.eq(idAfter)));
    }
    return field.gt(cursor).or(field.eq(cursor).and(idField.gt(idAfter)));
  }

  /** Instant 필드 커서 조건 */
  private BooleanExpression buildInstantCursorCondition(
      String cursor,
      UUID idAfter,
      com.querydsl.core.types.dsl.DateTimePath<Instant> field,
      com.querydsl.core.types.dsl.ComparablePath<UUID> idField,
      SortDirection sortDirection) {

    Instant c;
    try {
      c = Instant.parse(cursor);
    } catch (DateTimeParseException e) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
          .addDetail("reason", "잘못된 cursor 형식입니다 (Instant 필요)")
          .addDetail("cursor", cursor);
    }

    if (sortDirection == SortDirection.DESC) {
      return field.lt(c).or(field.eq(c).and(idField.lt(idAfter)));
    }
    return field.gt(c).or(field.eq(c).and(idField.gt(idAfter)));
  }

  /** Boolean 필드 커서 조건 */
  private BooleanExpression buildBooleanCursorCondition(
      String cursor,
      UUID idAfter,
      com.querydsl.core.types.dsl.BooleanPath field,
      com.querydsl.core.types.dsl.ComparablePath<UUID> idField,
      SortDirection sortDirection) {

    boolean c;
    try {
      c = Boolean.parseBoolean(cursor);
    } catch (Exception e) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
          .addDetail("reason", "잘못된 cursor 형식입니다 (Boolean 필요)")
          .addDetail("cursor", cursor);
    }

    if (sortDirection == SortDirection.DESC) {
      return field.lt(c).or(field.eq(c).and(idField.lt(idAfter)));
    }
    return field.gt(c).or(field.eq(c).and(idField.gt(idAfter)));
  }

  /** UserRole 필드 커서 조건 */
  private BooleanExpression buildRoleCursorCondition(
      String cursor,
      UUID idAfter,
      com.querydsl.core.types.dsl.EnumPath<UserRole> field,
      com.querydsl.core.types.dsl.ComparablePath<UUID> idField,
      SortDirection sortDirection) {

    UserRole c;
    try {
      c = UserRole.valueOf(cursor);
    } catch (IllegalArgumentException e) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
          .addDetail("reason", "잘못된 cursor 형식입니다 (UserRole 필요)")
          .addDetail("cursor", cursor);
    }

    if (sortDirection == SortDirection.DESC) {
      return field.lt(c).or(field.eq(c).and(idField.lt(idAfter)));
    }
    return field.gt(c).or(field.eq(c).and(idField.gt(idAfter)));
  }

  /** 기본 정렬 컬럼 */
  private OrderSpecifier<?> buildPrimaryOrder(SortBy sortBy, SortDirection sortDirection, QUser u) {

    return switch (sortBy) {
      case NAME -> (sortDirection == SortDirection.DESC) ? u.name.desc() : u.name.asc();
      case EMAIL -> (sortDirection == SortDirection.DESC) ? u.email.desc() : u.email.asc();
      case CREATED_AT ->
          (sortDirection == SortDirection.DESC) ? u.createdAt.desc() : u.createdAt.asc();
      case IS_LOCKED -> (sortDirection == SortDirection.DESC) ? u.locked.desc() : u.locked.asc();
      case ROLE -> (sortDirection == SortDirection.DESC) ? u.role.desc() : u.role.asc();
    };
  }

  // ===== 내부 Enum =====

  /** 정렬 기준 */
  private enum SortBy {
    NAME,
    EMAIL,
    CREATED_AT,
    IS_LOCKED,
    ROLE;

    public static SortBy from(String value) {
      if (value == null || value.isBlank()) {
        return CREATED_AT; // 기본값
      }
      return switch (value) {
        case "name" -> NAME;
        case "email" -> EMAIL;
        case "createdAt" -> CREATED_AT;
        case "isLocked" -> IS_LOCKED;
        case "role" -> ROLE;
        default ->
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
                .addDetail("reason", "잘못된 SortBy 값입니다.")
                .addDetail("sortBy", value);
      };
    }
  }

  /** 정렬 방향 */
  private SortDirection parseSortDirection(String value) {
    if (value == null || value.isBlank()) {
      return SortDirection.DESC;
    }

    String normalized = value.toUpperCase();

    if ("ASCENDING".equals(normalized)) {
      return SortDirection.ASC;
    } else if ("DESCENDING".equals(normalized)) {
      return SortDirection.DESC;
    }

    try {
      return SortDirection.valueOf(normalized);
    } catch (IllegalArgumentException e) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
          .addDetail("reason", "잘못된 sortDirection 값입니다. ASCENDING 또는 DESCENDING을 사용하세요.")
          .addDetail("sortDirection", value);
    }
  }
}
