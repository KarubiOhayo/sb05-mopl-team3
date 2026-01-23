package io.mopl.batch.client.tsdb;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.mopl.batch.client.tsdb.dto.TsdbEventsResponse;
import io.mopl.batch.client.tsdb.dto.TsdbSoccerResponse;
import io.mopl.batch.common.BatchErrorCode;
import io.mopl.core.error.BusinessException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** TheSportsDB API 호출을 담당하는 클라이언트. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TsdbApiClient {

  private final RestTemplate restTemplate;
  private final MeterRegistry meterRegistry;

  @Value("${tsdb.api-key}")
  private String apiKey;

  private static final String BASE_URL = "https://www.thesportsdb.com/api/v1/json";

  /**
   * The Sports DB API로부터 지정한 날짜의 축구(Soccer) 경기 목록을 조회한다.
   *
   * @param date 조회할 날짜 (yyyy-MM-dd)
   * @return 해당 날짜의 경기 목록. 결과가 없으면 빈 리스트
   */
  public List<TsdbSoccerResponse> fetchTodaySoccerMatches(String date) {
    String url = String.format("%s/%s/eventsday.php?d=%s&s=Soccer", BASE_URL, apiKey, date);

    Timer.Sample sample = Timer.start(meterRegistry);
    try {
      ResponseEntity<TsdbEventsResponse<TsdbSoccerResponse>> response =
          restTemplate.exchange(
              url, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<>() {});

      if (response.getBody() != null && response.getBody().getEvents() != null) {
        return response.getBody().getEvents();
      }
      return Collections.emptyList();
    } catch (Exception e) {
      Counter.builder("batch.external.api.errors")
          .tags("client", "tsdb", "operation", "soccer_today")
          .register(meterRegistry)
          .increment();
      log.error("TSDB API 호출 실패: {}", e.getMessage(), e);
      throw new BusinessException(BatchErrorCode.TSDB_API_CALL_ERROR);
    } finally {
      sample.stop(
          Timer.builder("batch.external.api.duration")
              .tags("client", "tsdb", "operation", "soccer_today")
              .register(meterRegistry));
    }
  }
}
