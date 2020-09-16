package io.kindx.dto.function;


import io.kindx.dto.BaseDto;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadabilityRequest extends BaseDto {
    @NonNull
    private String url;

    private String key;

    private boolean sanitize;
}
