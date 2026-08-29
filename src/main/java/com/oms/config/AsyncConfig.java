package com.oms.config;

import java.util.concurrent.Executor;

import com.oms.exception.AsyncExceptionHandler;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Async configuration with {@link EnableAsync} and a dedicated thread pool for email tasks.
 * <p>
 * Thread pool settings:
 * <ul>
 *   <li>Core pool size: 5</li>
 *   <li>Max pool size: 10</li>
 *   <li>Queue capacity: 20</li>
 *   <li>Thread name prefix: EmailExecutor-</li>
 * </ul>
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private ThreadPoolTaskExecutor emailExecutor;

    @Override
    public Executor getAsyncExecutor() {
        return emailExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }

    @Bean(name = "emailExecutor")
    public ThreadPoolTaskExecutor emailExecutor(Environment env) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(env.getProperty("app.email.thread-pool.core-size", Integer.class, 5));
        executor.setMaxPoolSize(env.getProperty("app.email.thread-pool.max-size", Integer.class, 10));
        executor.setQueueCapacity(env.getProperty("app.email.queue.capacity", Integer.class, 20));
        executor.setThreadNamePrefix("EmailExecutor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        this.emailExecutor = executor;
        return executor;
    }
}
