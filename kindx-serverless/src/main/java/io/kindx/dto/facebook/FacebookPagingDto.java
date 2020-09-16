package io.kindx.dto.facebook;

import io.kindx.dto.BaseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FacebookPagingDto extends BaseDto {

    private FacebookPageCursors cursors;

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static final class FacebookPageCursors extends BaseDto {
        private String before;
        private String after;
    }
}
