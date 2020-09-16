package io.kindx.backoffice.dto.menu;

import io.kindx.dto.BaseDto;
import lombok.*;


@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HtmlMenuProcessRequestDto extends BaseDto {
    @NonNull
    private String strippedText;

    @NonNull
    private String originalHtml;

    private boolean pdfSource;

    @NonNull
    private String url;

    @NonNull
    private String kitchenId;

    @NonNull
    String menuConfigurationId;

}
