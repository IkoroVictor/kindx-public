package io.kindx.entity.converter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import io.kindx.constants.Language;

import java.util.HashSet;
import java.util.Set;

public class LanguageSetConverter implements DynamoDBTypeConverter<Set<String>, Set<Language>> {

    @Override
    public Set<String> convert(Set<Language> languages) {
        Set<String> result = new HashSet<>();
        if (languages != null) {
            languages.forEach(e -> result.add(e.name()));
        }
        return result;
    }

    @Override
    public Set<Language>  unconvert(Set<String> strings) {
        Set<Language> result = new HashSet<>();
        if (strings != null) {
            strings.forEach(e -> result.add(Language.valueOf(e)));
        }
        return result;
    }
}