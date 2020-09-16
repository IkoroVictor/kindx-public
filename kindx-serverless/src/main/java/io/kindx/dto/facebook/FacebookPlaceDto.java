package io.kindx.dto.facebook;

import io.kindx.dto.BaseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FacebookPlaceDto extends BaseDto {

    private String id;
    private String name;
    private FacebookLocationDataDto location;

}
