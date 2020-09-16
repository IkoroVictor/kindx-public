package io.kindx.gateway.dto;

import io.kindx.dto.BaseDto;
import lombok.*;


@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuQueryDto extends BaseDto {
    private String kitchenId;
    private String pageToken;
}
