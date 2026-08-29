package com.oms.exception;

/**
 * @deprecated Replaced by domain-specific handlers:
 * {@link SecurityExceptionHandler}, {@link ValidationExceptionHandler},
 * {@link BusinessExceptionHandler}, {@link InfrastructureExceptionHandler}.
 * All extend {@link BaseExceptionHandler} for shared RFC 7807 construction.
 *
 * @see BaseExceptionHandler
 * @see SecurityExceptionHandler
 * @see ValidationExceptionHandler
 * @see BusinessExceptionHandler
 * @see InfrastructureExceptionHandler
 */
@Deprecated(forRemoval = true)
public final class GlobalExceptionHandler {

    private GlobalExceptionHandler() {
    }
}
