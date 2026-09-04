package com.bank.common.error;

import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The advice a service gets for free by depending on this library.
 *
 * <p>{@link PlatformErrorHandler} holds the behaviour; this exists only to carry
 * {@link RestControllerAdvice}, which Spring requires on the type rather than on a factory method.
 *
 * <p>A service needing handlers of its own extends {@link PlatformErrorHandler} and annotates its
 * own class; {@code PlatformErrorAutoConfiguration} then steps aside.
 */
@RestControllerAdvice
public class PlatformErrorAdvice extends PlatformErrorHandler {

    public PlatformErrorAdvice(ErrorHandlingSettings settings, ErrorRecorder recorder) {
        super(settings, recorder);
    }
}
