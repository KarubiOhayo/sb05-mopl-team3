package io.mopl.batch.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchJobMetricsListener implements JobExecutionListener {

  private static final String START_TIME_KEY = "metricsStartTimeNanos";
  private static final ConcurrentMap<String, AtomicLong> JOB_START_GAUGES =
      new ConcurrentHashMap<>();
  private static final ConcurrentMap<String, AtomicLong> JOB_END_GAUGES = new ConcurrentHashMap<>();

  private final MeterRegistry meterRegistry;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    jobExecution.getExecutionContext().putLong(START_TIME_KEY, System.nanoTime());
    String jobName = jobExecution.getJobInstance().getJobName();
    String runId = jobExecution.getJobParameters().getString("runId");
    String runTag = runId == null || runId.isBlank() ? "none" : runId;
    long nowMs = System.currentTimeMillis();
    AtomicLong gauge =
        JOB_START_GAUGES.computeIfAbsent(
            jobName + "|" + runTag,
            key -> {
              AtomicLong value = new AtomicLong();
              meterRegistry.gauge(
                  "batch.job.start.epoch_ms", Tags.of("job", jobName, "run_id", runTag), value);
              return value;
            });
    gauge.set(nowMs);
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    String jobName = jobExecution.getJobInstance().getJobName();
    String status = jobExecution.getStatus().toString();
    String runId = jobExecution.getJobParameters().getString("runId");
    String runTag = runId == null || runId.isBlank() ? "none" : runId;

    long startTime = jobExecution.getExecutionContext().getLong(START_TIME_KEY, System.nanoTime());
    long durationNanos = Math.max(0L, System.nanoTime() - startTime);
    Timer.builder("batch.job.duration")
        .tags("job", jobName, "status", status, "run_id", runTag)
        .register(meterRegistry)
        .record(durationNanos, TimeUnit.NANOSECONDS);

    long endMs = System.currentTimeMillis();
    AtomicLong endGauge =
        JOB_END_GAUGES.computeIfAbsent(
            jobName + "|" + runTag,
            key -> {
              AtomicLong value = new AtomicLong();
              meterRegistry.gauge(
                  "batch.job.end.epoch_ms", Tags.of("job", jobName, "run_id", runTag), value);
              return value;
            });
    endGauge.set(endMs);

    long readCount =
        jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getReadCount).sum();
    long writeCount =
        jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum();
    long filterCount =
        jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getFilterCount).sum();
    long skipCount =
        jobExecution.getStepExecutions().stream()
            .mapToLong(
                stepExecution ->
                    stepExecution.getReadSkipCount()
                        + stepExecution.getProcessSkipCount()
                        + stepExecution.getWriteSkipCount())
            .sum();

    DistributionSummary.builder("batch.job.items.read")
        .tags("job", jobName, "status", status, "run_id", runTag)
        .register(meterRegistry)
        .record(readCount);
    DistributionSummary.builder("batch.job.items.written")
        .tags("job", jobName, "status", status, "run_id", runTag)
        .register(meterRegistry)
        .record(writeCount);
    DistributionSummary.builder("batch.job.items.filtered")
        .tags("job", jobName, "status", status, "run_id", runTag)
        .register(meterRegistry)
        .record(filterCount);
    DistributionSummary.builder("batch.job.items.skipped")
        .tags("job", jobName, "status", status, "run_id", runTag)
        .register(meterRegistry)
        .record(skipCount);

    Counter.builder("batch.job.executions")
        .tags("job", jobName, "status", status, "run_id", runTag)
        .register(meterRegistry)
        .increment();
  }
}
