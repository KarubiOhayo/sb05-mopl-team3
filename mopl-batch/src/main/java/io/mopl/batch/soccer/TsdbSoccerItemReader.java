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

@Slf4j
@Component
@RequiredArgsConstructor
@StepScope
public class TsdbSoccerItemReader implements ItemReader<TsdbSoccerResponse> {

  private final TsdbApiClient tsdbApiClient;

  private final Queue<TsdbSoccerResponse> buffer = new LinkedList<>();

  @Override
  public @Nullable TsdbSoccerResponse read() {
    if (!buffer.isEmpty()) {
      return buffer.poll();
    }

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
