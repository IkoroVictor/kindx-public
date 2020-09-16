package io.kindx.dto;

import lombok.*;

import javax.validation.constraints.NotNull;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public  class GeoPointDto extends BaseDto {
    @NonNull
    @NotNull
    private Double lon;

    @NonNull
    @NotNull
    private Double lat;
}
