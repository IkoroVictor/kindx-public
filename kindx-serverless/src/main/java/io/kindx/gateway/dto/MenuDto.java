package io.kindx.gateway.dto;

import io.kindx.constants.Language;
import io.kindx.constants.MenuSource;
import io.kindx.dto.BaseDto;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

@Builder
@Data
@EqualsAndHashCode(callSuper = true)
public class MenuDto extends BaseDto {
    private String kitchenId;
    private String kitchenName;
    private String menuId;
    private String menu;
    private List<String> highlights;
    private Long postedTimestamp;
    private Double lat;
    private Double lon;
    private Long distanceInMeters;
    private MenuSource source;
    private String sourceUrl;
    private String address;
    private String fullAddress;
    private String thumbnailUrl;
    private String headerUrl;
    private String pageUrl;
    private String website;
    private Set<String> emails;
    private Set<String> phones;
    private Set<Language> languages;
    private Set<FoodItem> items;

    @Builder
    @Getter
    @EqualsAndHashCode(callSuper = true)
    public static final class FoodItem extends BaseDto {

        @NotNull
        private Long count;

        @NotNull
        private String name;

        private boolean preference;
    }
}
