package io.kindx.gateway.dto;

import io.kindx.constants.Language;
import io.kindx.constants.LocationSource;
import io.kindx.dto.BaseDto;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KitchenUpdateDto extends BaseDto {

    @NotNull
    @NotEmpty
    private Set<Language> languages;

    @NotBlank
    private String menuSignatureText;

    @NotNull
    @NotEmpty
    private List<MenuConfigurationDto> menuConfigurations;

    @NotNull
    @NotEmpty
    private String lineDelimiter;

    @NotNull
    @NotEmpty
    private String wordDelimiter;

    private String fbPageId;

    private String placesId;

    private LocationSource primaryLocationSource;


}
