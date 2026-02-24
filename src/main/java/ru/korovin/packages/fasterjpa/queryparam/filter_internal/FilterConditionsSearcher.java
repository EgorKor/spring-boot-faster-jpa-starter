package ru.korovin.packages.fasterjpa.queryparam.filter_internal;

import lombok.AllArgsConstructor;
import ru.korovin.packages.fasterjpa.exception.InvalidParameterException;
import ru.korovin.packages.fasterjpa.queryparam.Filter;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition.FilterCondition;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@AllArgsConstructor
public class FilterConditionsSearcher {
    public static final String FILTER_NOT_FOUND_MESSAGE = "В объекте %s , не найден фильтр с именем: %s";
    private Filter<?> filter;
    private Runnable originalNamesMapInitializer;


    public boolean containsFilterWithField(String field) {
        originalNamesMapInitializer.run();
        return filter.getConditionsWithNoMappedFields().containsKey(field);
    }

    public Optional<FilterCondition> findFirstFilterByName(String field) {
        originalNamesMapInitializer.run();
        if (!containsFilterWithField(field)) {
            return Optional.empty();
        }
        return filter.getConditionsWithNoMappedFields().get(field)
                .stream()
                .findFirst();
    }

    public FilterCondition getFirstFilterByFieldName(String field) {
        return findFirstFilterByName(field).orElseThrow(
                () -> new InvalidParameterException(FILTER_NOT_FOUND_MESSAGE.formatted(this, field))
        );
    }

    public Set<FilterCondition> getFiltersByFieldName(String field) {
        if (!containsFilterWithField(field)) {
            throw new InvalidParameterException(FILTER_NOT_FOUND_MESSAGE.formatted(this, field));
        }
        return filter.getConditionsWithNoMappedFields().get(field);
    }

    public Set<FilterCondition> getFiltersByFieldName(String field, Supplier<Set<FilterCondition>> defaultValueProducer) {
        if (!containsFilterWithField(field)) {
            return defaultValueProducer.get();
        }
        return filter.getConditionsWithNoMappedFields().get(field);
    }


}
