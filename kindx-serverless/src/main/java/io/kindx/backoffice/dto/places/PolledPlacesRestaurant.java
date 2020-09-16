package io.kindx.backoffice.dto.places;

import io.kindx.constants.Language;
import io.kindx.dto.BaseDto;
import io.kindx.dto.GeoPointDto;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PolledPlacesRestaurant extends BaseDto {
    @NonNull
    @NotBlank
    private String placesId;

    @NonNull
    @NotBlank
    private String locationId;

    @NonNull
    @NotBlank
    private String name;

    @NonNull
    @NotNull
    @Valid
    private GeoPointDto geoPoint;

    @NonNull
    @NotNull
    private Long createdTimestamp;

    private String address;

    private boolean validated;

    @Singular
    private Set<String> menuPageUrls;

    @Singular
    private Set<String> pdfUrls;

    @Singular
    @NotEmpty
    private Set<Language> defaultLanguages;

    @Singular
    @NotEmpty
    private Set<String> types;

    private String validationMessages;

    private String placeUrl;

    private String website;
    private String phone;
    private String internationalPhone;

    private String openingHours;
    private String facebookPageId;
    private String thumbnailUrl;
    private Float rating;
    private Integer totalRatings;
    private String placeType;

}
