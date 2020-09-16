package io.kindx.dto.facebook;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.kindx.dto.BaseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FacebookCoverDto extends BaseDto {

    private String id;

    @JsonProperty("cover_id")
    private String coverId;

    @JsonProperty("offset_x")
    private String offsetX;

    @JsonProperty("offset_y")
    private String offsetY;

    @JsonProperty("source")
    private String sourceUrl;

}
