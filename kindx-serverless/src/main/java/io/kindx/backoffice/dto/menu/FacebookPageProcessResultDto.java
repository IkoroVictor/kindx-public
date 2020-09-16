package io.kindx.backoffice.dto.menu;

import io.kindx.dto.BaseDto;
import lombok.*;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FacebookPageProcessResultDto extends BaseDto {
    private String kitchenId;

    private String menuConfigurationId;

    private String pageId;

    private String pageUsername;

    private boolean successful;

    private int polled;

    private int processed;

}
