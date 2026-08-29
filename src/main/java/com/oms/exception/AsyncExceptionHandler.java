package com.oms.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Handles uncaught exceptions thrown by {@code @Async} methods.
 * Logs the error with method details and parameters.
 */
@Slf4j
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("Uncaught exception in async method: {}#{}({})",
                method.getDeclaringClass().getSimpleName(),
                method.getName(),
                params != null ? Arrays.toString(params) : "void",
                ex);
    }
}
