package io.kindx.backoffice.dto.cognito;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class PostAuthCognitoRequest extends CognitoRequest {
    private Boolean newDeviceUsed;
    private Map<String, String> clientMetadata;
}
