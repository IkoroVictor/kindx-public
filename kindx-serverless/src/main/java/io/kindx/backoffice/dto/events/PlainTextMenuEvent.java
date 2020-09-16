package io.kindx.backoffice.dto.events;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlainTextMenuEvent extends Event {

    @NonNull
    private String kitchenId;

    private String text;

    @NonNull
    private String menuConfigurationId;
}
