package io.kindx.dto.facebook.webhook;

import io.kindx.dto.BaseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class FacebookWebhookEventDto extends BaseDto {
    private String object;
    private List<FacebookWebhookEventEntryDto> entry;
}
