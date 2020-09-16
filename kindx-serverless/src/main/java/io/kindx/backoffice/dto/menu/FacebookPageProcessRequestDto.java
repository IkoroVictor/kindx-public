package io.kindx.backoffice.dto.menu;

import io.kindx.dto.BaseDto;
import io.kindx.dto.facebook.FacebookPostDto;
import lombok.*;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FacebookPageProcessRequestDto extends BaseDto {

    @NonNull
    private String pageId;

    private FacebookPostDto post;

    @NonNull
    private String kitchenId;

    @NonNull
    String menuConfigurationId;
}
