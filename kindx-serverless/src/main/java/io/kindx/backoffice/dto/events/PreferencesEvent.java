package io.kindx.backoffice.dto.events;

import lombok.*;

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PreferencesEvent extends Event {

    @NonNull
    private String id;

    @NonNull
    private Set<String> preferences;

    @NonNull
    private Type type;

    private String menuId;

    private String kitchenId;

    private String userId;

    public enum Type { KITCHEN, MENU }

}
