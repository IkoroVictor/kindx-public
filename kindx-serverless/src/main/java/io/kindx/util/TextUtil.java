package io.kindx.util;

import io.kindx.constants.Language;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.stream.Stream;

public class TextUtil {

    public static String toSystemFriendlyText(String text)
    {
        return StringUtils.stripAccents(text)
                .replaceAll("-", " ")
                .replaceAll("[^a-zA-Z0-9\\u0400-\\u04FF\\u0500-\\u052F\\s+]", ""); //allow only Cyrillic, ascii alphabets and numbers
    }

    public static String toLocalesLowerCase(final String text, Iterable<Locale> locales) {
        String lowerCaseText = text;
        for (Locale locale : locales) {
            lowerCaseText = lowerCaseText.toLowerCase(locale);
        }
        return lowerCaseText;
    }

    public static String toLanguagesLowerCase(final String text, Iterable<Language> languages) {
        String lowerCaseText = text;
        for (Language language : languages) {
            lowerCaseText = lowerCaseText.toLowerCase(language.getLocale());
        }
        return lowerCaseText;
    }

    public static String cleanUpRedundantNewLines(String text) {
        return Stream.of(text.split("\\n"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .reduce("", (s, t) -> String.join("\n", s, t));
    }
}
