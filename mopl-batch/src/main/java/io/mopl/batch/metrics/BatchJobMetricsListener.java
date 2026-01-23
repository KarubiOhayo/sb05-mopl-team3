package io.mopl.batch.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchJobMetricsListener implements JobExecutionListener {

  private static final String START_TIME_KEY = "metricsStartTimeNanos";

  private final MeterRegistry meterRegistry;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    jobExecution.getExecutionContext().putLong(START_TIME_KEY, System.nanoTime());
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    String jobName = jobExecution.getJobInstance().getJobName();
    String status = jobExecution.getStatus().toString();

    long startTime = jobExecution.getExecutionContext().getLong(START_TIME_KEY, System.nanoTime());
    long durationNanos = Math.max(0L, System.nanoTime() - startTime);
    Timer.builder("batch.job.duration")
        .tags("job", jobName, "status", status)
        .register(meterRegistry)
        .record(durationNanos, TimeUnit.NANOSECONDS);

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
        .tags("job", jobName, "status", status)
        .register(meterRegistry)
        .record(readCount);
    DistributionSummary.builder("batch.job.items.written")
        .tags("job", jobName, "status", status)
        .register(meterRegistry)
        .record(writeCount);
    DistributionSummary.builder("batch.job.items.filtered")
        .tags("job", jobName, "status", status)
        .register(meterRegistry)
        .record(filterCount);
    DistributionSummary.builder("batch.job.items.skipped")
        .tags("job", jobName, "status", status)
        .register(meterRegistry)
        .record(skipCount);

    Counter.builder("batch.job.executions")
        .tags("job", jobName, "status", status)
        .register(meterRegistry)
        .increment();
  }
}
