package io.kindx.dto.facebook;

import io.kindx.dto.BaseDto;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FacebookLocationDataDto extends BaseDto {
    private String city;
    private String country;
    private Double latitude;
    private Double longitude;
    private String street;
    private String zip;
}
