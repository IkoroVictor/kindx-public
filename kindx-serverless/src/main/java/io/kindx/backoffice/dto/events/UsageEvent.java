package io.kindx.backoffice.dto.events;

import io.kindx.constants.UsageEventSource;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsageEvent<T> extends Event {
    @NonNull
    private String eventId;
    @NonNull
    private UsageEventSource source;
    @NonNull
    private Status status;
    @NonNull
    private Long createdTimestamp;

    @NonNull
    private String correlationId;

    private T meta;
    private String awsEventId;
    private String awsEventName;
    private String actor;
    public enum Status {
        FAILED, SUCCESS
    }
}
