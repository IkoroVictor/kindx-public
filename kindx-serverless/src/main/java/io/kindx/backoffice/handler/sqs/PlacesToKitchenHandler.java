package io.kindx.backoffice.handler.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.model.PlaceDetails;
import io.kindx.backoffice.dto.places.PolledPlacesRestaurant;
import io.kindx.backoffice.service.EventService;
import io.kindx.constants.Defaults;
import io.kindx.constants.LocationSource;
import io.kindx.constants.MenuConfigurationType;
import io.kindx.factory.InjectorFactory;
import io.kindx.gateway.dto.KitchenCreateDto;
import io.kindx.gateway.dto.MenuConfigurationDto;
import io.kindx.gateway.facade.admin.AdminKitchenFacade;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlacesToKitchenHandler extends SqsEventHandler<PolledPlacesRestaurant> {

    private static final Logger logger = LogManager.getLogger(PlacesToKitchenHandler.class);
    private final AdminKitchenFacade facade;


    public PlacesToKitchenHandler() {
        super(InjectorFactory.getInjector().getInstance(ObjectMapper.class),
                InjectorFactory.getInjector().getInstance(EventService.class),
                PolledPlacesRestaurant.class);
        this.facade = InjectorFactory.getInjector().getInstance(AdminKitchenFacade.class);
    }

    @Override
    @SneakyThrows
    protected Object processEventPayload(PolledPlacesRestaurant payload) {
        Map<String, Object> resultMap = new HashMap<>();
        boolean successful = false;

        try {
            resultMap.put("kitchenId", facade.createKitchenFromPlaceDetails(
                    mapToKitchenCreateDto(payload),
                    mapToPlaceDetails(payload),
                    Defaults.SYSTEM_USER_ID).getId());
            successful = true;
        } catch (Throwable ex) {
            logger.error("Could not create kitchen for polled place with id '{}' with name {} - {}",
                    payload.getPlacesId(), payload.getName(),  ex.getMessage(), ex);
            resultMap.put("error", ex.getMessage());
        }

        resultMap.put("success", successful);
        return resultMap;
    }

    @Override
    protected Object usagePayload(PolledPlacesRestaurant payload) {
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("placesId", payload.getPlacesId());
        payloadMap.put("name", payload.getName());
        payloadMap.put("locationId", payload.getLocationId());
        return payloadMap;
    }

    private KitchenCreateDto mapToKitchenCreateDto(PolledPlacesRestaurant restaurant) {
        List<MenuConfigurationDto> all = new ArrayList<>();

        if(restaurant.getMenuPageUrls() != null)  {
            List<MenuConfigurationDto> menuConfigurations = restaurant.getMenuPageUrls()
                    .stream()
                    .map(u -> MenuConfigurationDto
                            .builder()
                            .value(u)
                            .type(MenuConfigurationType.PAGE)
                            .build()).collect(Collectors.toList());
            all.addAll(menuConfigurations);
        }
        if (restaurant.getPdfUrls() != null) {
            List<MenuConfigurationDto> menuConfigurations = restaurant.getPdfUrls()
                    .stream()
                    .map(u -> MenuConfigurationDto
                            .builder()
                            .value(u)
                            .type(MenuConfigurationType.PDF_URL)
                            .build()).collect(Collectors.toList());
            all.addAll(menuConfigurations);
        }

        return KitchenCreateDto.builder()
                .fbPageId(restaurant.getFacebookPageId())
                .placesId(restaurant.getPlacesId())
                .languages(restaurant.getDefaultLanguages())
                .lineDelimiter(Defaults.LINE_DELIMITER_REGEX)
                .wordDelimiter(Defaults.WORD_DELIMITER_REGEX)
                .primaryLocationSource(LocationSource.GOOGLE_PLACES)
                .menuConfigurations(all)
                .menuSignatureText("")
                .build();
    }

    @SneakyThrows
    private PlaceDetails mapToPlaceDetails(PolledPlacesRestaurant payload) {
        PlaceDetails details = new PlaceDetails();
        details.name = payload.getName();
        details.website = new URL(payload.getWebsite());
        details.placeId = payload.getPlacesId();
        details.formattedPhoneNumber = payload.getPhone();
        details.internationalPhoneNumber = payload.getInternationalPhone();
        details.formattedAddress = payload.getAddress();
        return details;
    }
}
