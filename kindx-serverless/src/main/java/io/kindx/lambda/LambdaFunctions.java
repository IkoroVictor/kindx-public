package io.kindx.lambda;

import com.amazonaws.services.lambda.invoke.LambdaFunction;
import io.kindx.dto.function.ReadabilityRequest;
import io.kindx.dto.function.ReadabilityResponse;


public interface LambdaFunctions {

    @LambdaFunction(functionName = "READABILITY_PROCESSOR_ARN")
    ReadabilityResponse execReadabilityProcessor(ReadabilityRequest request);
}
