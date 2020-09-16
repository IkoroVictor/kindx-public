package io.kindx.backoffice.dto.events;

import lombok.*;

import java.util.Collection;


@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserMenuNotificationEvent extends Event {


    private String userId;

    private String menuId;

    private String kitchenId;

    private Collection<String> foodItems;
}
