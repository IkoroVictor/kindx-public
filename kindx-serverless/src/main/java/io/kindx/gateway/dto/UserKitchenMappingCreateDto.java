package io.kindx.gateway.dto;

import io.kindx.dto.BaseDto;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserKitchenMappingCreateDto extends BaseDto {

    private Set<String> preferences;

    @NotNull
    private Boolean shouldNotify;
}
