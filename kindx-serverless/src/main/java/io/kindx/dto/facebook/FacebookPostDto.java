package io.kindx.dto.facebook;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.kindx.dto.BaseDto;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FacebookPostDto extends BaseDto {
    private String id;

    @JsonProperty("created_time")
    private String createdTime;

    private String message;

    private String story;

    private FacebookPlaceDto place;

}
