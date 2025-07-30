package com.mafia.game.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class TaskSchedulerConfig {

    @Bean(destroyMethod="shutdown")
    public ScheduledExecutorService removalScheduler() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}