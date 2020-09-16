package io.kindx.gateway.dto;

import io.kindx.constants.Defaults;
import io.kindx.dto.BaseDto;
import io.kindx.dto.GeoPointDto;
import lombok.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuTodayQueryDto extends BaseDto {
    @NotNull
    private Integer zoneOffsetSeconds;
    @NotNull
    private GeoPointDto geoPoint;

    @Min(1)
    @Max(Defaults.MAX_PAGE_SIZE)
    @Builder.Default
    private Integer pageSize = Defaults.PAGE_SIZE;

    private String pageToken;
}
