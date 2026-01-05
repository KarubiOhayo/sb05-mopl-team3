package io.mopl.batch.controller;

import io.mopl.batch.common.BatchErrorCode;
import io.mopl.core.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배치 잡을 수동으로 트리거하기 위한 컨트롤러.
 *
 * <p>운영/테스트 환경에서 특정 수집 잡을 즉시 실행할 때 사용한다.
 */
@Slf4j
@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
public class JobLauncherController {

  private final JobOperator jobOperator;
  private final Job movieCollectJob;
  private final Job tvSeriesCollectJob;
  private final Job soccerCollectJob;

  /**
   * 영화 수집 배치 잡을 실행한다.
   *
   * @return 실행 요청 결과
   */
  @PostMapping("/movies")
  public ResponseEntity<String> runMovieCollectJob() {
    try {
      JobParameters jobParameters =
          new JobParametersBuilder()
              .addLong("requestTime", System.currentTimeMillis())
              .toJobParameters();

      jobOperator.start(movieCollectJob, jobParameters);

      return ResponseEntity.ok("Movie Collect Job Started!");
    } catch (Exception e) {
      log.error("배치 작업 실행 실패: jobName={}", movieCollectJob.getName(), e);
      throw new BusinessException(BatchErrorCode.JOB_LAUNCH_FAILED)
          .addDetail("jobName", movieCollectJob.getName());
    }
  }

  /**
   * TV 시리즈 수집 배치 잡을 실행한다.
   *
   * @return 실행 요청 결과
   */
  @PostMapping("/tv-series")
  public ResponseEntity<String> runTvSeriesCollectJob() {
    try {
      JobParameters jobParameters =
          new JobParametersBuilder()
              .addLong("requestTime", System.currentTimeMillis())
              .toJobParameters();

      jobOperator.start(tvSeriesCollectJob, jobParameters);

      return ResponseEntity.ok("Tv Series Collect Job Started!");
    } catch (Exception e) {
      log.error("배치 작업 실행 실패: jobName={}", tvSeriesCollectJob.getName(), e);
      throw new BusinessException(BatchErrorCode.JOB_LAUNCH_FAILED)
          .addDetail("jobName", tvSeriesCollectJob.getName());
    }
  }

  /**
   * 축구 경기 수집 배치 잡을 실행한다.
   *
   * @return 실행 요청 결과
   */
  @PostMapping("/soccer")
  public ResponseEntity<String> runSoccerCollectJob() {
    try {
      JobParameters jobParameters =
          new JobParametersBuilder()
              .addLong("requestTime", System.currentTimeMillis())
              .toJobParameters();

      jobOperator.start(soccerCollectJob, jobParameters);

      return ResponseEntity.ok("Soccer Collect Job Started!");
    } catch (Exception e) {
      log.error("배치 작업 실행 실패: jobName={}", soccerCollectJob.getName(), e);
      throw new BusinessException(BatchErrorCode.JOB_LAUNCH_FAILED)
          .addDetail("jobName", soccerCollectJob.getName());
    }
  }
}
