package io.kindx.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum Language {
    ESTONIAN("ee-et",Locale.forLanguageTag("ee-ET")),
    ENGLISH("en-gb", Locale.forLanguageTag("en-GB")),
    FINNISH("fi-fi", Locale.forLanguageTag("fi-FI")),
    LATVIAN("lv-lv", Locale.forLanguageTag("lv-LV")),
    RUSSIAN("ru-ru", Locale.forLanguageTag("ru-RU")),
    SWEDISH("sv-se", Locale.forLanguageTag("sv-SE"));

    private String value;
    private Locale locale;

    Language(String value, Locale locale) {
        this.value = value;
        this.locale = locale;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public Locale getLocale() {
        return locale;
    }

    @JsonCreator
    public static Language forValues(String value) {
        for (Language language : Language.values()) {
            if (language.getValue().equals(value)) {
                return language;
            }
        }
        return null;
    }
}
