package io.kindx.gateway.dto;

import io.kindx.dto.BaseDto;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LocationDto extends BaseDto {
    private String id;
    private String name;
    private double lat;
    private double lon;
}
