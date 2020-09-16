package io.kindx.dto.facebook;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.kindx.dto.BaseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FacebookPictureDto extends BaseDto {

    private String id;
    private PictureData data;

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static final class PictureData extends BaseDto {
        private Integer height;

        private Integer width;

        @JsonProperty("is_silhouette")
        private Boolean isSilhouette;

        private String url;
    }

}
