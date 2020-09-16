package io.kindx.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.MDC;

public class LogUtils {
    private static final String CORRELATION_ID_KEY = "correlationId";

    public static String getCorrelationId() {
        String correlationId  = (String) MDC.get(CORRELATION_ID_KEY);
        if (StringUtils.isBlank(correlationId)) {
            correlationId = IDUtil.generateCorrelationId();
            setCorrelationId(correlationId);
        }
        return correlationId;
    }


    public static void setCorrelationId(String id) {
        MDC.put(CORRELATION_ID_KEY, id);
    }
}
