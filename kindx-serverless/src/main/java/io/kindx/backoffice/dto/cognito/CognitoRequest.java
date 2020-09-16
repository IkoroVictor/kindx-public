package io.kindx.backoffice.dto.cognito;

import io.kindx.dto.BaseDto;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CognitoRequest extends BaseDto {
    private Map<String, String> userAttributes;
}
