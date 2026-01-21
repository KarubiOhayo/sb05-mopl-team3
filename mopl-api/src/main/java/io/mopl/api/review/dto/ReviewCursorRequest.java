package io.mopl.api.review.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewCursorRequest {

  private String cursor; // 커서값, 다음 페이지 조회 기준점
  private UUID idAfter; // 중복 방지용 id
  private Integer limit; // 요청 데이터 개수, 몇 개의 데이터 가져올 것인지?
  private String sortDirection; // 정렬 방향 - 오름차순, 내림차순
  private String sortBy; // 어떤 필드 기준으로 정렬할 지 - createdAt, likeCount

  public int getLimitOrDefault() {
    return limit == null ? 10 : limit;
  }
}
