package io.kindx.backoffice.processor.notification;

public interface NotificationProcessor<T> {
    T process(String identity, String body);
}
