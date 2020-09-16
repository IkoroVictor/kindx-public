package io.kindx.backoffice.dto.cognito;

import io.kindx.dto.BaseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CognitoTriggerEvent<S extends CognitoRequest, T extends CognitoResponse> extends BaseDto {
    private int version;
    private String triggerSource;
    private String region;
    private String userPoolId;
    private Map<String, String> callerContext;
    private S request;
    private T response;

}