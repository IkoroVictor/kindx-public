package io.kindx.gateway.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
public class ErrorDto {
    @NonNull
    private Integer code;
    @NonNull
    private String message;
}
