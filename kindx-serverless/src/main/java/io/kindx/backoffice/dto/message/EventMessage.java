package io.kindx.backoffice.dto.message;

import io.kindx.util.LogUtils;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class EventMessage<T> {

    private T payload;

    private ZonedDateTime messageTimestamp;

    private String id;
    private String correlationId;

    public EventMessage(){
        this(null, null, null);
    }
    public EventMessage(T payload, String id){
            this(payload, id, LogUtils.getCorrelationId());
    }

    public EventMessage(T payload, String id, String correlationId) {
        this.payload = payload;
        this.id = id;
        this.correlationId = correlationId;
        this.messageTimestamp = ZonedDateTime.now();
    }
}
