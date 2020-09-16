package io.kindx.dto.facebook;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.kindx.dto.BaseDto;
import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FacebookPageDto extends BaseDto {

    private String id;

    private String username;

    @JsonProperty("single_line_address")
    private String singleLineAddress;

    private String name;

    @JsonProperty("name_with_location_descriptor")
    private String nameWithLocationDescriptor;

    private String about;

    private String phone;

    private String website;

    private Set<String> emails;

    private FacebookLocationDataDto location;

    private FacebookPictureDto picture;

    @JsonProperty("is_always_open")
    private Boolean isAlwaysOpen;

    private PagePosts posts;

    private Map<String, String> hours;

    private FacebookCoverDto cover;


    @EqualsAndHashCode(callSuper = true)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagePosts extends BaseDto {
        @JsonProperty("data")
        private List<FacebookPostDto> posts;

        private FacebookPagingDto paging;
    }

}
