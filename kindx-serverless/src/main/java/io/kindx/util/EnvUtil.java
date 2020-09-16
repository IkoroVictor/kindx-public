package io.kindx.util;

import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;

public class EnvUtil {

    public static <T> T getEnvOrDefault(String env, Function<String, T> mapper,  T defaultValue) {
        String s = System.getenv(env);
        return StringUtils.isNotBlank(s) ? mapper.apply(s) : defaultValue;
    }

    public static <T> T getEnv(String env, Function<String, T> mapper) {
        return getEnvOrDefault(env, mapper, null);
    }

    public static String getEnvOrDefault(String env, String defaultValue) {
        String s = System.getenv(env);
        return StringUtils.isNotBlank(s) ? s : defaultValue;
    }

    public static String getEnv(String env) {
        return getEnvOrDefault(env, null);
    }


    public static Long getEnvLongOrDefault(String env, Long defaultValue) {
        return getEnvOrDefault(env, Long::parseLong, defaultValue);
    }

    public static Long getEnvLong(String env) {
       return getEnvLongOrDefault(env, null);
    }


    public static Float getEnvFloatOrDefault(String env, Float defaultValue) {
        return getEnvOrDefault(env, Float::parseFloat, defaultValue);
    }

    public static Float getEnvFloat(String env) {
        return getEnvFloatOrDefault(env, null);
    }


    public static boolean getEnvBoolOrDefault(String env, boolean defaultValue) {
        return getEnvOrDefault(env, Boolean::parseBoolean, defaultValue);
    }

    public static boolean getEnvBool(String env) {
        return getEnvBoolOrDefault(env, false);
    }




}
