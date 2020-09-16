package io.kindx.backoffice.dto.events;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlacesPollEvent extends  Event {
    @NonNull
    private String id;

    @NonNull
    private String locationId;

    @NonNull
    private Double lat;

    @NonNull
    private Double lon;

    @NonNull
    private Integer radiusInMeters;

    private String name;

}
