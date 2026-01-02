package io.mopl.api.playlist.service.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;

import io.mopl.api.content.domain.QContent;
import io.mopl.api.content.domain.QContentTag;
import io.mopl.api.content.domain.QTag;
import io.mopl.api.content.dto.ContentSummary;
import io.mopl.api.playlist.domain.QPlaylistContent;
import lombok.RequiredArgsConstructor;

/**
 * 플레이리스트 목록(여러 개)에 대해,
 * 각 플레이리스트에 들어있는 콘텐츠 목록 + 태그까지 "묶어서" 로딩해주는 Loader.
 *
 * 핵심 목표:
 * - N개의 플레이리스트에 대해 콘텐츠를 N번 조회(N+1)하지 않고,
 * - IN 쿼리 + 최소한의 쿼리 횟수로 필요한 데이터만 가져온 다음,
 * - Java에서 playlistId별로 묶어서(Map) 반환한다.
 */
@Component
@RequiredArgsConstructor
public class PlaylistContentLoader {

	private final JPAQueryFactory queryFactory;

	/**
	 * @param playlistIds 현재 페이지에서 조회된 플레이리스트 ID 목록
	 * @return key=playlistId, value=해당 플레이리스트의 콘텐츠 요약 리스트
	 */
	public Map<UUID, List<ContentSummary>> loadContentsByPlaylistIds(List<UUID> playlistIds) {
		// 조회 대상이 없으면 쿼리X
		if (playlistIds == null || playlistIds.isEmpty()) {
			return Map.of();
		}
		QPlaylistContent pc = QPlaylistContent.playlistContent;
		QContent c = QContent.content;


		// 1) playlist_contents + contents를 조인해서
		List<Tuple> rows = queryFactory
			.select(
				pc.id.playlistId,
				pc.id.contentId,

				c.type,
				c.title,
				c.description,
				c.thumbnailUrl,
				c.averageRating,
				c.reviewCount
			)
			.from(pc)
			.join(c).on(pc.id.contentId.eq(c.id))
			.where(pc.id.playlistId.in(playlistIds))
			.orderBy(pc.id.playlistId.asc(), pc.addedAt.desc())
			.fetch();

		// playlistId -> contentId 리스트 (순서 유지)
		Map<UUID, List<UUID>> contentIdsByPlaylistId = new HashMap<>();

		// contentId -> content 기본정보 임시 저장(태그 붙이기 전)
		Map<UUID, ContentBase> baseByContentId = new HashMap<>();

		// 태그 조회를 위해 전체 contentId를 Set으로 수집
		Set<UUID> allContentIds = new HashSet<>();

		for (Tuple row : rows) {
			UUID playlistId = row.get(pc.id.playlistId);
			UUID contentId = row.get(pc.id.contentId);

			contentIdsByPlaylistId.computeIfAbsent(playlistId, k -> new ArrayList<>()).add(contentId);

			allContentIds.add(contentId);

			// content 기본정보는 contentId당 1번만 저장
			if (!baseByContentId.containsKey(contentId)) {
				Double avg = row.get(c.averageRating);
				double avgValue = avg != null ? avg.doubleValue() : 0.0d;
				Integer reviewCount = row.get(c.reviewCount);

				ContentBase base = new ContentBase(
					contentId,
					row.get(c.type),
					row.get(c.title),
					row.get(c.description),
					row.get(c.thumbnailUrl),
					avgValue,
					reviewCount != null ? reviewCount.intValue() : 0
				);
				baseByContentId.put(contentId, base);
			}
		}

		// 콘텐츠가 하나도 없으면 playlistId별 빈 리스트를 넣어서 반환
		if (allContentIds.isEmpty()) {
			Map<UUID, List<ContentSummary>> emptyResult = new HashMap<UUID, List<ContentSummary>>();
			for (UUID playlistId : playlistIds) {
				emptyResult.put(playlistId, List.of());
			}
			return emptyResult;
		}


		 // 2) content_tags + tags 조인해서 "콘텐츠별 태그 목록"을 한 번에 가져온다.
		QContentTag ct = QContentTag.contentTag; // content_tags
		QTag t = QTag.tag;                       // tags

		List<Tuple> tagRows = queryFactory
			.select(ct.id.contentId, t.name)
			.from(ct)
			.join(t).on(ct.id.tagId.eq(t.id))
			.where(ct.id.contentId.in(allContentIds))
			.fetch();

		// contentId -> tagName 리스트
		Map<UUID, List<String>> tagsByContentId = new HashMap<UUID, List<String>>();
		for (Tuple row : tagRows) {
			UUID contentId = row.get(ct.id.contentId);
			String tagName = row.get(t.name);

			tagsByContentId.computeIfAbsent(contentId, k -> new ArrayList<>()).add(tagName);
		}

		// 3) contentId -> ContentSummary(태그 포함) 생성
		Map<UUID, ContentSummary> summaryByContentId = new HashMap<UUID, ContentSummary>();
		for (Map.Entry<UUID, ContentBase> entry : baseByContentId.entrySet()) {
			UUID contentId = entry.getKey();
			ContentBase base = entry.getValue();

			List<String> tags = tagsByContentId.get(contentId);
			if (tags == null) {
				tags = List.of();
			}

			ContentSummary summary = ContentSummary.builder()
				.id(base.id)
				.type(base.type)
				.title(base.title)
				.description(base.description)
				.thumbnailUrl(base.thumbnailUrl)
				.tags(tags)
				.averageRating(base.averageRating)
				.reviewCount(base.reviewCount)
				.build();

			summaryByContentId.put(contentId, summary);
		}

		// 4) playlistId -> List<ContentSummary>로 최종 조립(playlist_contents 순서 유지)
		Map<UUID, List<ContentSummary>> result = new HashMap<UUID, List<ContentSummary>>();
		for (UUID playlistId : playlistIds) {
			List<UUID> contentIds = contentIdsByPlaylistId.get(playlistId);

			if (contentIds == null || contentIds.isEmpty()) {
				result.put(playlistId, List.of());
				continue;
			}

			List<ContentSummary> summaries = new ArrayList<ContentSummary>();
			for (UUID contentId : contentIds) {
				ContentSummary summary = summaryByContentId.get(contentId);
				if (summary != null) {
					summaries.add(summary);
				}
			}
			result.put(playlistId, summaries);
		}

		return result;
	}

	// 5. 태그 붙이기 전 "콘텐츠 기본 정보"를 잠깐 담는 내부 클래스. (DB 조회 결과를 저장해두고, tags와 결합해 최종 DTO를 만들기 위함)
	private record ContentBase(
		UUID id,
		io.mopl.api.content.domain.ContentType type,
		String title,
		String description,
		String thumbnailUrl,
		double averageRating,
		int reviewCount
	) {}
}
