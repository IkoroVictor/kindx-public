package io.kindx.backoffice.dto.user;


import io.kindx.dto.BaseDto;
import io.kindx.dto.GeoPointDto;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserLastLocation extends BaseDto {
    @NonNull
    String userId;

    @NonNull
    GeoPointDto geoPoint;

    @NonNull
    private Long createdTimestamp;

}
