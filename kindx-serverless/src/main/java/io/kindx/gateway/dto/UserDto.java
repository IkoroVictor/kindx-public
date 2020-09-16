package io.kindx.gateway.dto;

import io.kindx.backoffice.processor.notification.NotificationChannel;
import io.kindx.dto.BaseDto;
import io.kindx.dto.GeoPointDto;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Set;

@Builder
@Data
@EqualsAndHashCode(callSuper = true)
public class UserDto extends BaseDto {

    private String userId;

    private Set<String> generalFoodPreferences;

    private Long lastActivityTimestamp;

    private NotificationChannel notificationChannel;

    private String locale;

    private List<LocationDto> locations;

    private GeoPointDto userLastLocation;
}
