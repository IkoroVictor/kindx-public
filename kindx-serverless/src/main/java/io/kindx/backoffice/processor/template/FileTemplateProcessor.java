package io.kindx.backoffice.processor.template;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import lombok.SneakyThrows;

import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

public class FileTemplateProcessor implements TemplateProcessor {
    private Configuration templateConfiguration;

    public FileTemplateProcessor() {
        templateConfiguration = new Configuration(Configuration.VERSION_2_3_29);
        templateConfiguration.setClassForTemplateLoading(
                this.getClass(), "/templates");
        templateConfiguration.setLocalizedLookup(true);
        templateConfiguration.setDefaultEncoding("UTF-8");
        templateConfiguration.setLocale(Locale.ENGLISH);
        templateConfiguration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }


    @SneakyThrows
    public String process(String templateKey, Locale locale, Map<String,Object> valueMap) {
        String templateName = (templateKey + ".ftl").toLowerCase();
        Template template = templateConfiguration.getTemplate(templateName, locale);
        StringWriter writer = new StringWriter();
        template.process(valueMap, writer);
        return writer.toString();
    }
}
