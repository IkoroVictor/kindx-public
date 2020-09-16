package io.kindx.util;

import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.control.Try;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.function.Supplier;

public class ResilienceUtil {

    private static final Logger logger = LogManager.getLogger(ResilienceUtil.class);

    private static final RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(100))
            .retryOnException(e -> e instanceof Exception)
            .build();

    public  static <T> T retryOnException(Supplier<T> supplier) {
        return Try.ofSupplier(Decorators.ofSupplier(supplier)
                .withRetry(Retry.of("Retry", retryConfig))
                .decorate())
                .get();
    }

    public static void retryOnException(Runnable runnable) {
        retryOnException(() -> {runnable.run(); return true;});
    }


    public static void retryOnExceptionSilently(Runnable runnable) {
        try {
            retryOnException(runnable);
        } catch (Exception ex) { logger.error(ex.getMessage(), ex);}
    }
}
