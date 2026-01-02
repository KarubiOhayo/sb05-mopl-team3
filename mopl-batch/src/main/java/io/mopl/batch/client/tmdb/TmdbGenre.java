package io.mopl.batch.client.tmdb;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TmdbGenre {
  ACTION(28, "액션"),
  ADVENTURE(12, "모험"),
  ANIMATION(16, "애니메이션"),
  COMEDY(35, "코미디"),
  CRIME(80, "범죄"),
  DOCUMENTARY(99, "다큐멘터리"),
  DRAMA(18, "드라마"),
  FAMILY(10751, "가족"),
  FANTASY(14, "판타지"),
  HISTORY(36, "역사"),
  HORROR(27, "공포"),
  MUSIC(10402, "음악"),
  MYSTERY(9648, "미스터리"),
  ROMANCE(10749, "로맨스"),
  SCIENCE_FICTION(878, "SF"),
  TV_MOVIE(10770, "TV 영화"),
  THRILLER(53, "스릴러"),
  WAR(10752, "전쟁"),
  WESTERN(37, "서부"),

  // TV Only Genres
  ACTION_ADVENTURE(10759, "액션 & 어드벤처"),
  KIDS(10762, "키즈"),
  NEWS(10763, "뉴스"),
  REALITY(10764, "리얼리티"),
  SCI_FI_FANTASY(10765, "SF & 판타지"),
  SOAP(10766, "소프"),
  TALK(10767, "토크쇼"),
  WAR_POLITICS(10768, "전쟁 & 정치");

  private final int id;
  private final String name;

  public static String getNameById(int id) {
    return Arrays.stream(values())
        .filter(genre -> genre.id == id)
        .findFirst()
        .map(TmdbGenre::getName)
        .orElse("기타");
  }
}
