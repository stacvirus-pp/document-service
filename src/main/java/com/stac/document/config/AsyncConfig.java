package com.stac.document.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
  @Bean(name = "taskExecutor")
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5); // 5 threads always active
    executor.setMaxPoolSize(20); // Up to 20 threads for parallel uploads
    executor.setQueueCapacity(50); // Queue up to 50 tasks
    executor.setThreadNamePrefix("AsyncUpload-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // Run in caller thread if pool is full
    executor.initialize();
    return executor;
  }
}
