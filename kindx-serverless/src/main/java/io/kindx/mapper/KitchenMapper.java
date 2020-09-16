package io.kindx.mapper;

import io.kindx.entity.Kitchen;
import io.kindx.entity.KitchenConfiguration;
import io.kindx.entity.MenuConfiguration;
import io.kindx.gateway.dto.KitchenDto;
import io.kindx.gateway.dto.MenuConfigurationDto;

import java.util.List;
import java.util.stream.Collectors;

public class KitchenMapper {

    public KitchenDto toKitchenDto(Kitchen kitchen,
                                   KitchenConfiguration configuration,
                                   List<MenuConfiguration> menuConfigurations) {
        return KitchenDto.builder()
                .id(kitchen.getKitchenId())
                .googlePlacesId(kitchen.getGooglePlacesId())
                .fbPageId(kitchen.getFacebookId())
                .createdTimestamp(kitchen.getCreatedTimestamp())
                .updatedTimestamp(kitchen.getUpdatedTimestamp())
                .pageUrl(kitchen.getPageUrl())
                .defaultDisplayAddress(kitchen.getDefaultDisplayAddress())
                .defaultDisplayName(kitchen.getDefaultDisplayName())
                .fallbackThumbnailUrl(kitchen.getFallbackThumbnailUrl())
                .isDisabled(kitchen.getIsDisabled())
                .languages(configuration.getLanguages())
                .menuSignatureText(configuration.getMenuSignatureText())
                .lineDelimiter(configuration.getLineDelimiterRegex())
                .wordDelimiter(configuration.getWordDelimiterRegex())
                .primaryLocationSource(configuration.getPrimaryLocationSource())
                .menuConfigurations(
                        menuConfigurations
                        .stream().map(this::mapMenuConfig)
                        .collect(Collectors.toList()))
                .createdBy(kitchen.getCreatedBy())
                .lastUpdatedBy(kitchen.getLastUpdatedBy())
                .build();
    }

    public MenuConfigurationDto mapMenuConfig(MenuConfiguration config) {
        return MenuConfigurationDto.builder()
                .type(config.getType())
                .value(config.getValue())
                .build();
    }
}
