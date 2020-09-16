package io.kindx.dto.facebook.webhook;

import io.kindx.dto.BaseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class FacebookWebhookEventChangeDto extends BaseDto {
    private String field;
    private Map<String, Object> value;
}
