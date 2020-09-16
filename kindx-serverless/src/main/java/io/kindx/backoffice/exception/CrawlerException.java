package io.kindx.backoffice.exception;

public class CrawlerException extends RuntimeException {
    public CrawlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
