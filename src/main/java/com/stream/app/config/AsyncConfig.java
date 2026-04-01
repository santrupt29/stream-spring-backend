package com.stream.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {

    /**
     * Bounded thread pool for video processing.
     * On a 2GB VM, limit to 2 concurrent FFmpeg processes to avoid OOM.
     * Each FFmpeg process + S3 upload can consume ~300-500MB.
     */
    @Bean(name = "videoProcessingExecutor")
    public ExecutorService videoProcessingExecutor() {
        return Executors.newFixedThreadPool(2);
    }
}
