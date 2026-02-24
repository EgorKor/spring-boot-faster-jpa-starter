package ru.korovin.packages.fasterjpa.queryparam.filter_internal;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import ru.korovin.packages.fasterjpa.annotations.AllowedOperations;
import ru.korovin.packages.fasterjpa.annotations.ParamCountLimit;
import ru.korovin.packages.fasterjpa.exception.InvalidParameterException;
import ru.korovin.packages.fasterjpa.queryparam.Filter;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.visitor.FilterCountConditionsVisitor;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.visitor.FilterIterationVisitor;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.visitor.FilterOperationIndexVisitor;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@AllArgsConstructor
public class FilterValidator {
    private Filter<?> filter;
    private Runnable originalNamesMapInitializer;

    public void validateAndApplyAllies() {
        validateFields();
        validateOperations();
        applyAllies();
    }

    @SneakyThrows
    public void applyAllies() {
        if (filter.isCalledByInheritor()) {
            return;
        }
        Field[] fields = filter.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.getType() != Supplier.class) {
                continue;
            }
            String alliesName = ((Supplier<String>) field.get(filter)).get();
            String fieldName = field.getName();
            String regexSafeFieldName = Pattern.quote(fieldName);

            filter.getFilterCondition().visitWith(new FilterIterationVisitor(
                    (condition) -> {
                        String beforeAlliesApply = condition.property();
                        condition.property(beforeAlliesApply.replaceFirst(regexSafeFieldName, alliesName));
                    }
            ));
        }
    }

    public void validateFields() {
        if (filter.isCalledByInheritor()) {
            return;
        }
        int conditionsCount = filter.getFilterCondition().visitWith(
                new FilterCountConditionsVisitor()
        );
        FilterConditionsSearcher searcher = filter.searcher();
        ParamCountLimit limit;
        if ((limit = this.getClass().getAnnotation(ParamCountLimit.class)) != null
                && limit.value() != ParamCountLimit.UNLIMITED
                && conditionsCount > limit.value()) {
            throw new InvalidParameterException("Недопустимое общее кол-во фильтров: " + conditionsCount
                    + ". Допустимое значение: " + limit.value());
        }
        originalNamesMapInitializer.run();
        Set<String> paramsNames = new HashSet<>(filter.getConditionsWithNoMappedFields().keySet());

        Field[] declaredFields = filter.getClass().getDeclaredFields();
        Set<String> allowedFields = Arrays.stream(declaredFields)
                .map(f -> {
                    String paramName = f.getName();

                    ParamCountLimit paramLimit = f.getAnnotation(ParamCountLimit.class);
                    if (paramLimit != null && searcher.containsFilterWithField(paramName)
                            && searcher.getFiltersByFieldName(paramName, Set::of).size() > paramLimit.value()) {
                        throw new InvalidParameterException("Недопустимое кол-во фильтров для параметра %s: "
                                .formatted(paramName) + conditionsCount + ". Допустимое значение: " + paramLimit.value());
                    }
                    return paramName;
                })
                .collect(Collectors.toSet());

        paramsNames.removeAll(allowedFields);
        filter.getPropertiesWhiteList().forEach(paramsNames::remove);
        if (!paramsNames.isEmpty()) {
            throw new InvalidParameterException("Недопустимые параметры фильтрации: " + paramsNames);
        }
    }


    @SneakyThrows
    public void validateOperations() {
        if (filter.isCalledByInheritor()) {
            return;
        }
        Map<String, Set<FilterOperation>> index = filter.getFilterCondition()
                .visitWith(new FilterOperationIndexVisitor());
        Field[] fields = filter.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            if (!field.isAnnotationPresent(AllowedOperations.class)) {
                continue;
            }

            String paramName = ((Supplier<String>) field.get(filter)).get();

            AllowedOperations allowedOperationsAnnotation = field.getAnnotation(AllowedOperations.class);

            if (index.containsKey(paramName)) {
                Set<FilterOperation> usedOperations = index.get(paramName);
                Set<FilterOperation> allowedOperations = Arrays.stream(allowedOperationsAnnotation.value())
                        .collect(Collectors.toSet());

                for (FilterOperation usedOp : usedOperations) {
                    if (!allowedOperations.contains(usedOp)) {
                        throw new InvalidParameterException("Недопустимая операция " + usedOp + " для параметра " + field.getName());
                    }
                }
            }
        }
    }
}
