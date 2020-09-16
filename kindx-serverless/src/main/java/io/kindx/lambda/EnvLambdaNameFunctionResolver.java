package io.kindx.lambda;

import com.amazonaws.services.lambda.invoke.DefaultLambdaFunctionNameResolver;
import com.amazonaws.services.lambda.invoke.LambdaFunction;
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactoryConfig;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;

import static io.kindx.util.EnvUtil.getEnv;

public class EnvLambdaNameFunctionResolver extends DefaultLambdaFunctionNameResolver {

    @Override
    public String getFunctionName(Method method, LambdaFunction annotation, LambdaInvokerFactoryConfig config) {
        String arn = getEnv(annotation.functionName());
        return StringUtils.isNotBlank(arn)
                ? arn
                : super.getFunctionName(method, annotation, config);
    }
}
