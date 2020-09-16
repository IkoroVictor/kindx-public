package io.kindx.backoffice.dto.events;

import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MenuCrawlEvent extends Event {
    private String kitchenId;
    private String menuConfigurationId;
    private String url;
    private ContentType contentType;
    public enum ContentType { HTML, PDF}
}
