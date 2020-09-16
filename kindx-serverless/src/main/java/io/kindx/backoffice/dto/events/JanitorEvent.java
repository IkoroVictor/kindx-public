package io.kindx.backoffice.dto.events;

import io.kindx.constants.JanitorEventType;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JanitorEvent extends Event {
    @NonNull
    private JanitorEventType type;

    @NonNull
    private String value;

    private String kitchenId;

    private String menuId;

    private String userId;

}
