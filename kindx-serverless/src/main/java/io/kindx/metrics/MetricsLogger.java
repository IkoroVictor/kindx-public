package io.kindx.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MetricsLogger {

    private static final Logger logger = LogManager.getLogger(MetricsLogger.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void logMetrics(Class clazz, double value, String operation, Map<String, String> tags) {
        logMetrics(clazz.getName(), value, operation, tags);
    }


    public static void logMetrics(String metricRef, double value,
                                  String operation, Map<String, String> tags) {
        operation = operation == null ? "" : ("." + operation.toLowerCase());
        log(String.format("kindx.ops.%s%s", metricRef, operation), value, tags);
    }

    @SneakyThrows
    private static void log(String ref, double value,  Map<String, String> tags){
        Map<String, Object> metric = new HashMap<>();
        metric.put("m", ref);
        metric.put("v", value);
        metric.put("e", System.currentTimeMillis() / 1000);
        metric.put("t", Collections.emptyList());
        if (tags != null  && !tags.isEmpty()) {
            metric.put("t", tags.entrySet()
                    .stream()
                    .map(e -> String.format("%s:%s", e.getKey().toLowerCase(), e.getValue().toLowerCase()))
                    .toArray());
        }
        //https://docs.datadoghq.com/integrations/amazon_lambda/?tab=javanetandcustomruntimes
        String metricString = mapper.writeValueAsString(metric);
        System.out.println(metricString);
        //logger.info(metricString);
    }
}
