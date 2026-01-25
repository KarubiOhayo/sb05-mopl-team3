package io.mopl.worker.common.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class MeteredCallerRunsPolicy implements RejectedExecutionHandler {

  private final Counter rejectedCounter;
  private final RejectedExecutionHandler delegate = new ThreadPoolExecutor.CallerRunsPolicy();
  private final String executorName;

  MeteredCallerRunsPolicy(MeterRegistry meterRegistry, String executorName) {
    this.executorName = executorName;
    this.rejectedCounter =
        Counter.builder("worker.kafka.executor.rejected")
            .tags("executor", executorName)
            .register(meterRegistry);
  }

  @Override
  public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
    rejectedCounter.increment();
    log.warn(
        "Executor rejected task; falling back to caller thread. executor={}, active={}, queueSize={}, poolSize={}",
        executorName,
        e.getActiveCount(),
        e.getQueue() == null ? -1 : e.getQueue().size(),
        e.getPoolSize());
    delegate.rejectedExecution(r, e);
  }
}
