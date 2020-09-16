package io.kindx.backoffice.dto.events;


import io.kindx.dto.GeoPointDto;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PreferenceReprocessEvent extends Event {
    @NonNull
    private String id;

    @NonNull
    private ReprocessType type;

    private String kitchenId;

    private String menuId;

    private String userId;

    private GeoPointDto pointOfFocus;

    private Long searchRadiusInMeters;


    public enum ReprocessType {
        KITCHEN_MENU_UPDATE,
        USER_LOCATION_UPDATE,
        USER_PREFERENCE_UPDATE
    }
}
