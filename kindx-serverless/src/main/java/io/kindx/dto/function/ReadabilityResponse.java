package io.kindx.dto.function;


import io.kindx.dto.BaseDto;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadabilityResponse extends BaseDto {

    private boolean success;
    private boolean readable;
    private String url;
    private String message;
    private String contentKey;
}
