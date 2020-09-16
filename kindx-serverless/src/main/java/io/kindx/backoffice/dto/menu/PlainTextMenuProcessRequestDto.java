package io.kindx.backoffice.dto.menu;

import io.kindx.dto.BaseDto;
import lombok.*;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PlainTextMenuProcessRequestDto extends BaseDto {
    @NonNull
    private String text;

    @NonNull
    private String kitchenId;

    @NonNull
    String menuConfigurationId;

}
