package io.kindx.gateway.dto;


import io.kindx.constants.Language;
import io.kindx.constants.LocationSource;
import io.kindx.dto.BaseDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Set;

@Builder
@Getter
public class KitchenDto extends BaseDto {

    private String id;

    private String fbPageId;

    private String googlePlacesId;

    private Long createdTimestamp;

    private Long updatedTimestamp;

    private String createdBy;

    private String lastUpdatedBy;

    private String pageUrl;

    private String defaultDisplayName;

    private String defaultDisplayAddress;

    private String fallbackThumbnailUrl;

    private Set<Language> languages;

    private String menuSignatureText;

    private List<MenuConfigurationDto> menuConfigurations;

    private LocationSource primaryLocationSource;

    private String lineDelimiter;

    private String wordDelimiter;

    private Boolean isDisabled;
}
