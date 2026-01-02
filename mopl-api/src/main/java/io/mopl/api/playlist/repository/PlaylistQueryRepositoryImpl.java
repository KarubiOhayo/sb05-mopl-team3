package io.mopl.api.playlist.repository;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import io.mopl.api.playlist.domain.Playlist;
import io.mopl.api.playlist.domain.QPlaylist;
import io.mopl.api.playlist.domain.QPlaylistSubscription;
import io.mopl.core.error.BusinessException;
import io.mopl.core.error.CommonErrorCode;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PlaylistQueryRepositoryImpl implements PlaylistQueryRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public PlaylistPage findPlaylistsPage(
		String keywordLike,
		UUID ownerIdEqual,
		UUID subscriberIdEqual,
		String cursor,
		UUID idAfter,
		int limit,
		String sortDirectionRaw,
		String sortByRaw
	) {
		// 1) 정렬 기준 / 방향 파싱 (문자열을 enum으로 바꿔서 분기 실수 줄임)
		SortBy sortBy = SortBy.from(sortByRaw);
		SortDirection sortDirection = SortDirection.from(sortDirectionRaw);

		// 2) QueryDSL Q타입: 테이블을 자바 객체처럼 쓰기 위한 메타 모델
		QPlaylist p = QPlaylist.playlist;

		// 3) where 조건들을 동적으로 조립할 BooleanBuilder
		// - request 파라미터가 null이면 조건을 추가하지 않음
		BooleanBuilder where = buildBaseWhere(keywordLike, ownerIdEqual, subscriberIdEqual, p);

		// 4) 커서 페이징 조건(cursor + idAfter)
		// - 커서는 "정렬 키(updatedAt 또는 subscribeCount)의 마지막 값"
		// - idAfter는 "동일한 정렬 키 값이 있을 때 중복/누락 방지용 tie-breaker"
		if (cursor != null && !cursor.isBlank()) {
			if (idAfter == null) {
				// 커서 페이징에서 cursor만 있고 idAfter가 없으면
				// 동일 updatedAt(또는 동일 subscribeCount)에서 중복/누락이 생길 수 있어서 강제.
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
					.addDetail("reason", "cursor가 있으면 idAfter도 필수입니다.");
			}
			BooleanExpression cursorCondition = buildCursorCondition(cursor, idAfter, sortBy, sortDirection, p);
			where.and(cursorCondition);
		}

		// 5) 정렬(Order By)
		//    - 기본 정렬 키 1개 + id로 2차 정렬을 반드시 둬야 커서 페이징이 안정적으로 동작함
		OrderSpecifier<?> primaryOrder = buildPrimaryOrder(sortBy, sortDirection, p);

		// 6) limit+1로 가져오는 이유:
		//    - 딱 limit개만 가져오면 다음 페이지 존재 여부를 알기 어려움
		//    - limit+1개를 가져와서 하나 더 있으면 hasNext=true
		List<Playlist> fetched = queryFactory
			.selectFrom(p)
			.where(where)
			.orderBy(primaryOrder, (sortDirection == SortDirection.DESC) ? p.id.desc() : p.id.asc())
			.limit(limit + 1)
			.fetch();

		boolean hasNext = fetched.size() > limit;
		if (hasNext) {
			// limit+1개 중 마지막 1개는 다음 페이지 존재 확인용이므로 잘라냄
			fetched = fetched.subList(0, limit);
		}

		// 7) nextCursor/nextIdAfter 계산
		//    - 다음 페이지 요청 시 이 값을 그대로 넘기면 됨
		String nextCursor = null;
		UUID nextIdAfter = null;

		if (hasNext && !fetched.isEmpty()) {
			Playlist last = fetched.get(fetched.size() - 1);

			nextIdAfter = last.getId();
			if (sortBy == SortBy.UPDATED_AT) {
				// Instant는 ISO-8601 문자열로 변환해서 전달 (예: 2025-12-30T...)
				nextCursor = String.valueOf(last.getUpdatedAt());
			} else {
				// subscriberCount는 숫자 문자열로 전달 (예: "10")
				nextCursor = String.valueOf(last.getSubscriberCount());
			}
		}

		return new PlaylistPage(fetched, hasNext, nextCursor, nextIdAfter);
	}

	@Override
	public long countPlaylists(String keywordLike, UUID ownerIdEqual, UUID subscriberIdEqual) {
		QPlaylist p = QPlaylist.playlist;

		// 리스트 조회와 "같은 조건"으로 count해야 totalCount가 맞음
		BooleanBuilder where = buildBaseWhere(keywordLike, ownerIdEqual, subscriberIdEqual, p);

		Long count = queryFactory
			.select(p.count())
			.from(p)
			.where(where)
			.fetchOne();

		return count != null ? count.longValue() : 0L;
	}

	/**
	 * keywordLike / ownerIdEqual / subscriberIdEqual 같은 "기본 필터"를 만드는 곳
	 * - 이 메서드가 리스트 조회 / count 조회 둘 다에서 재사용됨
	 */
	private BooleanBuilder buildBaseWhere(
		String keywordLike,
		UUID ownerIdEqual,
		UUID subscriberIdEqual,
		QPlaylist p
	) {
		BooleanBuilder where = new BooleanBuilder();

		// 1) 제목 검색 (대소문자 무시)
		if (keywordLike != null && !keywordLike.isBlank()) {
			where.and(p.title.containsIgnoreCase(keywordLike));
		}

		// 2) owner 필터
		if (ownerIdEqual != null) {
			where.and(p.ownerId.eq(ownerIdEqual));
		}

		// 3) subscriber 필터: "특정 유저가 구독한 플레이리스트만"
		// - playlists 테이블에 subscription 정보가 없으므로
		// - playlist_subscriptions에 존재하는지 exists로 필터링함
		if (subscriberIdEqual != null) {
			QPlaylistSubscription s = QPlaylistSubscription.playlistSubscription;

			where.and(
				JPAExpressions.selectOne()
					.from(s)
					.where(
						s.id.playlistId.eq(p.id)
							.and(s.id.userId.eq(subscriberIdEqual))
					)
					.exists()
			);
		}

		return where;
	}

	/**
	 * 커서 페이징 조건 만들기
	 * - DESC면 "더 작은 값"으로 내려가야 다음 페이지
	 * - ASC면 "더 큰 값"으로 올라가야 다음 페이지
	 * - 정렬키가 같은 경우를 대비해 id로 tie-breaker
	 */
	private BooleanExpression buildCursorCondition(
		String cursor,
		UUID idAfter,
		SortBy sortBy,
		SortDirection sortDirection,
		QPlaylist p
	) {
		if (sortBy == SortBy.UPDATED_AT) {
			Instant c;
			try {
				c = Instant.parse(cursor);
			} catch (DateTimeParseException e) {
				throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
					.addDetail("reason", "잘못된 cursor 형식입니다")
					.addDetail("cursor", cursor);
			}

			if (sortDirection == SortDirection.DESC) {
				// (updatedAt < cursor) OR (updatedAt == cursor AND id < idAfter)
				return p.updatedAt.lt(c)
					.or(p.updatedAt.eq(c).and(p.id.lt(idAfter)));
			}
			// ASC
			return p.updatedAt.gt(c)
				.or(p.updatedAt.eq(c).and(p.id.gt(idAfter)));
		}

		// sortBy == SUBSCRIBE_COUNT
		long c;
		try {
			c = Long.parseLong(cursor);
		} catch (NumberFormatException e) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
				.addDetail("reason", "잘못된 cursor 형식입니다")
				.addDetail("cursor", cursor);
		}

		if (sortDirection == SortDirection.DESC) {
			return p.subscriberCount.lt(c)
				.or(p.subscriberCount.eq(c).and(p.id.lt(idAfter)));
		}
		return p.subscriberCount.gt(c)
			.or(p.subscriberCount.eq(c).and(p.id.gt(idAfter)));
	}

	/**
	 * primary 정렬(Order by의 첫 번째 컬럼)
	 */
	private OrderSpecifier<?> buildPrimaryOrder(SortBy sortBy, SortDirection sortDirection, QPlaylist p) {
		if (sortBy == SortBy.UPDATED_AT) {
			return (sortDirection == SortDirection.DESC) ? p.updatedAt.desc() : p.updatedAt.asc();
		}
		return (sortDirection == SortDirection.DESC) ? p.subscriberCount.desc() : p.subscriberCount.asc();
	}

	/**
	 * 요청 파라미터 문자열을 내부 enum으로 변환
	 * - 오타/허용되지 않는 값이면 바로 예외로 알려주기
	 */
	private enum SortBy {
		UPDATED_AT,
		SUBSCRIBE_COUNT;

		static SortBy from(String raw) {
			if (raw == null) {
				// 기본값을 팀 컨벤션으로 정해도 됨
				return UPDATED_AT;
			}
			if ("updatedAt".equals(raw))
				return UPDATED_AT;
			if ("subscribeCount".equals(raw))
				return SUBSCRIBE_COUNT;
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
				.addDetail("sortBy", String.valueOf(raw));
		}
	}

	private enum SortDirection {
		ASC,
		DESC;

		static SortDirection from(String raw) {
			if (raw == null) {
				return DESC;
			}
			if ("ASCENDING".equals(raw))
				return ASC;
			if ("DESCENDING".equals(raw))
				return DESC;
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST)
				.addDetail("sortDirection", String.valueOf(raw));
		}
	}
}

