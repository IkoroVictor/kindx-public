package io.kindx.backoffice.dto.events;

import io.kindx.dto.facebook.FacebookPostDto;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FacebookTaggedPostEvent extends Event {
    @NonNull
    private String kitchenId;

    @NonNull
    private String facebookId;

    @NonNull
    private FacebookPostDto post;

    @NonNull
    private String menuConfigurationId;
}
