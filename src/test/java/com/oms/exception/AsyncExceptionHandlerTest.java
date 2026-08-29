package com.oms.exception;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.assertj.core.api.Assertions.assertThat;

class AsyncExceptionHandlerTest {

    private final AsyncExceptionHandler handler = new AsyncExceptionHandler();

    @Test
    void handleUncaughtException_shouldNotThrowException() {
        Method method = String.class.getMethods()[0];
        assertDoesNotThrow(() -> 
            handler.handleUncaughtException(new RuntimeException("test"), method, "param1", "param2"));
    }

    @Test
    void handleUncaughtException_shouldHandleNullParams() {
        Method method = String.class.getMethods()[0];
        assertDoesNotThrow(() -> 
            handler.handleUncaughtException(new RuntimeException("test"), method, (Object[]) null));
    }

    @Test
    void handleUncaughtException_shouldHandleNullException() {
        Method method = String.class.getMethods()[0];
        assertDoesNotThrow(() -> 
            handler.handleUncaughtException(null, method, "param"));
    }

    @Test
    void handler_shouldImplementAsyncUncaughtExceptionHandler() {
        assertThat(handler).isInstanceOf(org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler.class);
    }
}
