package io.kindx.gateway.dto;

import io.kindx.constants.MenuConfigurationType;
import io.kindx.dto.BaseDto;
import lombok.*;

import javax.validation.constraints.NotNull;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MenuConfigurationDto extends BaseDto {

    @NotNull
    private MenuConfigurationType type;

    @NotNull
    private String value;
}
