package io.mopl.batch.soccer;

import io.mopl.batch.client.tsdb.TsdbApiClient;
import io.mopl.batch.client.tsdb.dto.TsdbSoccerResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.stereotype.Component;

/**
 * TheSportsDB에서 오늘(UTC 기준)의 축구 경기 목록을 한 번만 가져오는 Reader.
 *
 * <p>첫 호출에만 API를 호출하고, 이후에는 내부 버퍼를 소진한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@StepScope
public class TsdbSoccerItemReader implements ItemReader<TsdbSoccerResponse> {

  private final TsdbApiClient tsdbApiClient;

  private final Queue<TsdbSoccerResponse> buffer = new LinkedList<>();
  private boolean fetched = false;

  /**
   * 다음 경기 항목을 반환한다.
   *
   * @return 다음 경기 항목, 더 이상 없으면 null
   */
  @Override
  public @Nullable TsdbSoccerResponse read() {
    if (!buffer.isEmpty()) {
      return buffer.poll();
    }

    if (fetched) {
      return null;
    }
    fetched = true;

    String date = LocalDate.now(ZoneOffset.UTC).toString();
    log.info("TSDB API 호출 date: {}", date);
    List<TsdbSoccerResponse> soccer = tsdbApiClient.fetchTodaySoccerMatches(date);

    if (soccer == null || soccer.isEmpty()) {
      return null;
    }

    buffer.addAll(soccer);
    return buffer.poll();
  }
}
