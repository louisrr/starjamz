package com.play.stream.Starjams.MediaIngressService.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Dedicated thread pools for GStreamer pipeline execution and fan-out writes.
 *
 * <p>GStreamer pipelines must NEVER block Spring MVC request threads.
 * All pipeline construction, state transitions, and bus event handling
 * run exclusively on {@code gstreamerExecutor}.
 */
@Configuration
public class AsyncConfig {

    @Value("${async.gstreamer-core-pool-size:20}")
    private int gstreamerCorePoolSize;

    @Value("${async.gstreamer-max-pool-size:50}")
    private int gstreamerMaxPoolSize;

    @Value("${async.fanout-core-pool-size:10}")
    private int fanoutCorePoolSize;

    @Value("${async.fanout-max-pool-size:30}")
    private int fanoutMaxPoolSize;

    @Value("${async.queue-capacity:500}")
    private int queueCapacity;

    @Bean(name = "gstreamerExecutor")
    public Executor gstreamerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(gstreamerCorePoolSize);
        executor.setMaxPoolSize(gstreamerMaxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("gst-ingest-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean(name = "fanoutExecutor")
    public Executor fanoutExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(fanoutCorePoolSize);
        executor.setMaxPoolSize(fanoutMaxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("media-fanout-");
        executor.initialize();
        return executor;
    }
}
