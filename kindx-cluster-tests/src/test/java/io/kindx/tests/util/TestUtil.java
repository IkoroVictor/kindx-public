package io.kindx.tests.util;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.util.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class TestUtil {


    private static final Logger logger = LogManager.getLogger(TestUtil.class);

    public static EnvironmentVariables loadTestEnvVariables(String envFileName) {
        EnvironmentVariables environmentVariables = new EnvironmentVariables();
        List<String[]> pairs = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(
                        ClassLoader.getSystemResourceAsStream(envFileName))))
                .lines()
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .filter(s -> s.contains("="))
                .map(s -> s.split("="))
                .collect(toList());

        pairs.forEach(pair -> {
            if (StringUtils.isBlank(System.getenv(pair[0]))) {
                environmentVariables.set(pair[0], pair[1]);
            }
        });
        return environmentVariables;
    }

    public static String loadTextFileResource(String fileName) {
        return new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(
                        ClassLoader.getSystemResourceAsStream(fileName))))
                .lines()
                .reduce("", (s, v) -> String.join("\n", s, v));
    }


    public static void logAWSResponse(Response<String> response) {
        logAWSRequest((Request<String>) response.getHttpResponse().getRequest());
        logger.info("==========================AWS RESPONSE==========================\n\n");

        logger.info("[Status]: {}", response.getHttpResponse().getStatusCode());
        logger.info("[Status Text]: {}", response.getHttpResponse().getStatusText());
        logger.info("[Body]: {}", response.getAwsResponse());

        List<String> headers = response.getHttpResponse().getHeaders().entrySet()
                .stream()
                .map(k -> String.join("=", k.getKey(), k.getValue())).collect(toList());

        headers.forEach(h -> logger.info("[Header]: {}", h));
        logger.info("===========================END AWS RESPONSE==========================");

    }

    public static void logAWSRequest(Request<String> request) {
        logger.info("==========================AWS REQUEST==========================");

        logger.info("[Endpoint]: {}", request.getEndpoint().toString());
        logger.info("[Path]: {}", request.getResourcePath());
        logger.info("[Method]: {}", request.getHttpMethod().name());
        logger.info("[Service]: {}", request.getServiceName());
        logger.info("[Params]: {}", request.getServiceName());

        List<String> headers = request.getHeaders().entrySet()
                .stream()
                .map(k -> String.join("=", k.getKey(), k.getValue())).collect(toList());

        List<String> params = request.getParameters().entrySet()
                .stream()
                .map(k -> String.join("=", k.getKey(), String.join(",", k.getValue())))
                .collect(toList());

        headers.forEach(h -> logger.info("[Header]: {}", h));
        params.forEach(p -> logger.info("[Param]: {} ", p));
        try {
            logger.info("[Body]: {}", IOUtils.toString(request.getContent()));
        } catch (Exception e) {
            logger.info("[Body]: {}", "**<Error logging body>**");
        }
        logger.info("=========================END AWS REQUEST===========================");

    }

    public static void waitForProcessing(long milli) throws Exception {
        logger.info("\n\n Waiting '{}' seconds for processing \n\n", (int)(milli/1000));
        Thread.sleep(milli);
    }


}
