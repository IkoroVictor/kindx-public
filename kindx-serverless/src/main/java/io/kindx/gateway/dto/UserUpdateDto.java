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
public class UserUpdateDto extends BaseDto {

    private Set<String> generalFoodPreferences;

    @NotNull
    private String locale;
}
