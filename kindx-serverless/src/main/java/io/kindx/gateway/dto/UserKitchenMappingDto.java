package io.kindx.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Builder
@Data
public class UserKitchenMappingDto {

    private String kitchenId;

    private String userId;

    private Set<String> preferences;

    private Boolean shouldNotify;
}
