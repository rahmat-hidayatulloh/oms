package com.oms.constant;

/**
 * MDC (Mapped Diagnostic Context) key constants for structured logging.
 */
public final class MDCConstants {

    private MDCConstants() {
    }

    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";
    public static final String METHOD = "method";
    public static final String PATH = "path";
}
