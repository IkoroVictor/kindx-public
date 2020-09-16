package io.kindx.dto.facebook.webhook;

import io.kindx.dto.BaseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class FacebookWebhookEventEntryDto  extends BaseDto {
    private String id;
    private String uid;
    private String timestamp;
    private List<FacebookWebhookEventChangeDto> changes;

}
