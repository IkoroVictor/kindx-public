package io.kindx.backoffice.processor.template;

import java.util.Locale;
import java.util.Map;

public interface TemplateProcessor {

    String process(String templateKey, Locale locale, Map<String,Object> valueMap);
}
