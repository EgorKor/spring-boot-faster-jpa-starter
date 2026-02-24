package ru.korovin.packages.fasterjpa.queryparam;

import jakarta.persistence.criteria.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import ru.korovin.packages.fasterjpa.annotations.AllowedOperations;
import ru.korovin.packages.fasterjpa.annotations.ParamCountLimit;
import ru.korovin.packages.fasterjpa.exception.InvalidParameterException;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.FilterBuilder;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.FilterOperation;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.Is;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.condition.*;
import ru.korovin.packages.fasterjpa.queryparam.filter_internal.visitor.*;
import ru.korovin.packages.fasterjpa.service.Joins;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ru.korovin.packages.fasterjpa.queryparam.factories.Filters.fb;
import static ru.korovin.packages.fasterjpa.queryparam.filter_internal.FilterOperation.IS;

/**
 * Параметр запроса для фильтрации запрашиваемых ресурсов.
 *
 * @author EgorKor
 * @version 1.0.4
 * @since 2025
 */
//TODO: добавить поддержку операций работы с JSON
//TODO: добавить поддержку функций size() length() для SQL
//TODO: реализовать метод обновления по фильтру
@Slf4j
@Setter
@Getter
public class Filter<T> implements Specification<T> {
    public static final String FILTER_NOT_FOUND_MESSAGE = "В объекте %s , не найден фильтр с именем: %s";
    protected Class<?> entityType;
    protected boolean isDistinct;
    protected List<Consumer<Root<T>>> queryConfigurers = new ArrayList<>();
    private Set<String> fieldWhiteList = new HashSet<>();
    private Set<String> fetchingProperties = new HashSet<>();

    private FilterConditionTreeNode filterCondition;

    public Filter() {
        this.filterCondition = new FilterEmptyCondition();
        determineEntityType();
    }

    public Filter(Class<?> entityType) {
        this.filterCondition = new FilterEmptyCondition();
        this.entityType = entityType;
    }

    public Filter(@NonNull FilterConditionTreeNode filterCondition) {
        this.filterCondition = filterCondition;
        determineEntityType();
    }

    public Filter(@NonNull FilterConditionTreeNode filterCondition,
                  @NonNull Class<?> entityType) {
        this.filterCondition = filterCondition;
        this.entityType = entityType;
    }

    @SneakyThrows
    public <R extends Filter<?>> R copy() {
        R copiedFilter = (R) this.getClass().getDeclaredConstructor().newInstance();
        copiedFilter.setEntityType(entityType);
        copiedFilter.setFieldWhiteList(fieldWhiteList);
        //FIXME
        copiedFilter.setFilterCondition(filterCondition);
        copiedFilter.setDistinct(isDistinct);
        return copiedFilter;
    }

    public static FilterBuilder builder() {
        return fb;
    }

    public static <T extends Filter<?>> T softDeleteFilter(Field field, boolean isDeleted) {
        return softDeleteFilter(field.getName(), field.getType(), isDeleted);
    }

    public static <T extends Filter<?>> T softDeleteFilter(Field field, boolean isDeleted, Class<T> entityType) {
        T softDeleteFilter = softDeleteFilter(field.getName(), field.getType(), isDeleted);
        softDeleteFilter.setEntityType(entityType);
        return softDeleteFilter;
    }

    public static <T extends Filter<?>> T softDeleteFilter(String fieldName, Class<?> fieldType, boolean isDeleted) {
        T filter = (T) new Filter<>();
        FilterCondition filterCondition;
        if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
            filterCondition = new FilterCondition(fieldName, IS, isDeleted);
        } else {
            filterCondition = new FilterCondition(fieldName, IS, isDeleted ? Is.NOT_NULL : Is.NULL);
        }
        filter.setFilterCondition(filterCondition);
        return filter;
    }

    public static <T> Filter<T> empty() {
        return new Filter<>();
    }

    public static <T> Filter<T> empty(Class<T> entityType) {
        return new Filter<>(entityType);
    }

    private static Predicate getComparisonPredicate(CriteriaBuilder cb,
                                                    FilterOperation operation,
                                                    Expression<Comparable> comparableSelection,
                                                    Comparable value) {
        return switch (operation) {
            case GT -> cb.greaterThan(comparableSelection, value);
            case LS -> cb.lessThan(comparableSelection, value);
            case GTE -> cb.greaterThanOrEqualTo(comparableSelection, value);
            case LSE -> cb.lessThanOrEqualTo(comparableSelection, value);
            default -> throw new InvalidParameterException("Некорректное операция сравнения: " + operation);
        };
    }

    public static <X> Expression<X> getTypedExpression(Expression<?> expression, Class<X> type) {
        return (Expression<X>) expression;
    }

    public boolean isFiltered() {
        return filterCondition != null && !(filterCondition instanceof FilterEmptyCondition);
    }

    public boolean isUnfiltered() {
        return filterCondition == null || filterCondition instanceof FilterEmptyCondition;
    }


    //region Criteria API Mapping
    @Override
    public Predicate toPredicate(Root<T> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder cb) {
        query.distinct(isDistinct);
        return toPredicate(root, cb);
    }

    public Predicate toPredicate(Root<T> root,
                                 CriteriaBuilder cb) {
        //конфигурация запроса
        queryConfigurers.forEach(c -> c.accept(root));
        return filterCondition.parsePredicate(root, null, cb, entityType);
    }

    public <R extends Filter<?>> R configureQuery(Consumer<Root<T>> queryConfigurer) {
        queryConfigurers.add(queryConfigurer);
        return _this();
    }

    public <R extends Filter<?>> R not(){
        this.filterCondition = new FilterNotCondition(
                this.filterCondition
        );
        return _this();
    }



    public <R extends Filter<?>> R andCondition(FilterConditionTreeNode conditionTreeNode){
        return andFilter(conditionTreeNode.toFilter());
    }

    public <R extends Filter<?>> R andFilter(Filter<?> externalFilter) {
        this.initializeOriginalNamesMap();
        this.filterCondition = new FilterAndCondition(
                List.of(
                        filterCondition,
                        externalFilter.getFilterCondition()
                )
        );
        this.fieldWhiteList.addAll(
                externalFilter.getFilterCondition()
                        .visitWith(new FilterListConditionsVisitor())
                        .stream()
                        .map(FilterCondition::property)
                        .toList()
        );
        if (externalFilter.conditionsWithNoMappedFields != null) {
            externalFilter.conditionsWithNoMappedFields.forEach(
                    (field, filters) -> {
                        if (this.conditionsWithNoMappedFields.containsKey(field)) {
                            this.conditionsWithNoMappedFields.get(field).addAll(filters);
                        } else {
                            this.conditionsWithNoMappedFields.put(field, filters);
                        }
                    }
            );
        }
        return _this();
    }

    public int getConditionsCount(){
        return filterCondition.visitWith(new FilterCountConditionsVisitor());
    }

    public List<FilterCondition> getConditions(){
        return filterCondition.visitWith(new FilterListConditionsVisitor());
    }

    public <R extends Filter<?>> R orCondition(FilterConditionTreeNode conditionTreeNode){
        return orFilter(conditionTreeNode.toFilter());
    }

    public <R extends Filter<?>> R orFilter(Filter<?> externalFilter){
        this.initializeOriginalNamesMap();
        this.filterCondition = new FilterOrCondition(
                List.of(
                        filterCondition,
                        externalFilter.getFilterCondition()
                )
        );
        this.fieldWhiteList.addAll(
                externalFilter.getFilterCondition()
                        .visitWith(new FilterListConditionsVisitor())
                        .stream()
                        .map(FilterCondition::property)
                        .toList()
        );
        if (externalFilter.conditionsWithNoMappedFields != null) {
            externalFilter.conditionsWithNoMappedFields.forEach(
                    (field, filters) -> {
                        if (this.conditionsWithNoMappedFields.containsKey(field)) {
                            this.conditionsWithNoMappedFields.get(field).addAll(filters);
                        } else {
                            this.conditionsWithNoMappedFields.put(field, filters);
                        }
                    }
            );
        }
        return _this();
    }

    private void initializeOriginalNamesMap() {
        if (this.conditionsWithNoMappedFields == null) {
            this.conditionsWithNoMappedFields = filterCondition.visitWith(
                    new FilterPropertyIndexVisitor()
            );
        }
    }

    public <R extends Filter<?>> R distinct() {
        this.isDistinct = true;
        return _this();
    }

    public <R extends Filter<?>> R withFetchJoin(String fetchingProperty) {
        this.fetchingProperties.add(fetchingProperty);
        queryConfigurers.add((root) -> {
            String[] attributes = fetchingProperty.split("\\.");
            FetchParent<?, ?> currentParent = root;

            for (String attribute : attributes) {
                currentParent = currentParent.fetch(attribute, JoinType.LEFT);
            }
        });
        return _this();
    }

    public <R extends Filter<?>> R withFetchJoins(Joins joins) {
        this.fetchingProperties.addAll(joins.properties());
        joins.properties().forEach(fetchingProperty -> {
            queryConfigurers.add((root) -> {
                String[] attributes = fetchingProperty.split("\\.");
                FetchParent<?, ?> currentParent = root;

                for (String attribute : attributes) {
                    currentParent = currentParent.fetch(attribute, JoinType.LEFT);
                }
            });
        });
        return _this();
    }

    //endregion


    //region Utility Methods

    public boolean containsFilterWithField(String field) {
        initializeOriginalNamesMap();
        return conditionsWithNoMappedFields.containsKey(field);
    }

    public Optional<FilterCondition> findFirstFilterByName(String field) {
        initializeOriginalNamesMap();
        if (!containsFilterWithField(field)) {
            return Optional.empty();
        }
        return conditionsWithNoMappedFields.get(field)
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
        return conditionsWithNoMappedFields.get(field);
    }

    public Set<FilterCondition> getFiltersByFieldName(String field, Supplier<Set<FilterCondition>> defaultValueProducer) {
        if (!containsFilterWithField(field)) {
            return defaultValueProducer.get();
        }
        return conditionsWithNoMappedFields.get(field);
    }

    private Predicate parseContainsPredicate(CriteriaBuilder cb, Expression<?> selection, String stringValue) {
        Expression<String> stringSelection = cb.lower(getTypedExpression(selection, String.class));
        return cb.like(stringSelection, "%" + stringValue.toLowerCase() + "%");
    }

    private Predicate parseLikePredicate(CriteriaBuilder cb, Expression<?> selection, String stringValue) {
        Expression<String> stringPath = getTypedExpression(selection, String.class);
        return cb.like(stringPath, stringValue);
    }

    private void determineEntityType() {
        if (getClass() == Filter.class) {
            return;
        }
        try {
            Type superclass = getClass().getGenericSuperclass();
            ParameterizedType parameterizedType = (ParameterizedType) superclass;
            Type typeArgument = parameterizedType.getActualTypeArguments()[0];
            this.entityType = typeArgument.getClass();
        } catch (Exception e) {
            log.warn("Cannot determine entity type", e);
        }
    }

    private <SameType extends Filter<?>> SameType _this() {
        return (SameType) this;
    }

    private Map<String, Set<FilterCondition>> conditionsWithNoMappedFields;

    public void validateAndApplyAllies() {

        validateFields();
        validateOperations();
        applyAllies();
    }

    @SneakyThrows
    public void applyAllies() {
        if (this.getClass() == Filter.class) {
            return;
        }
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.getType() != Supplier.class) {
                continue;
            }
            String alliesName = ((Supplier<String>) field.get(this)).get();
            String fieldName = field.getName();
            String regexSafeFieldName = Pattern.quote(fieldName);

            filterCondition.visitWith(new FilterIterationVisitor(
                    (condition) -> {
                        String beforeAlliesApply = condition.property();
                        condition.property(beforeAlliesApply.replaceFirst(regexSafeFieldName, alliesName));
                    }
            ));
        }
    }

    public void validateFields() {
        if (this.getClass() == Filter.class) {
            return;
        }
        int conditionsCount = filterCondition.visitWith(
                new FilterCountConditionsVisitor()
        );

        ParamCountLimit limit;
        if ((limit = this.getClass().getAnnotation(ParamCountLimit.class)) != null
                && limit.value() != ParamCountLimit.UNLIMITED
                && conditionsCount > limit.value()) {
            throw new InvalidParameterException("Недопустимое общее кол-во фильтров: " + conditionsCount
                    + ". Допустимое значение: " + limit.value());
        }
        initializeOriginalNamesMap();
        Set<String> paramsNames = new HashSet<>(conditionsWithNoMappedFields.keySet());

        Field[] declaredFields = this.getClass().getDeclaredFields();
        Set<String> allowedFields = Arrays.stream(declaredFields)
                .map(f -> {
                    String paramName = f.getName();

                    ParamCountLimit paramLimit = f.getAnnotation(ParamCountLimit.class);
                    if (paramLimit != null && containsFilterWithField(paramName)
                            && getFiltersByFieldName(paramName, Set::of).size() > paramLimit.value()) {
                        throw new InvalidParameterException("Недопустимое кол-во фильтров для параметра %s: "
                                .formatted(paramName) + conditionsCount + ". Допустимое значение: " + paramLimit.value());
                    }
                    return paramName;
                })
                .collect(Collectors.toSet());

        paramsNames.removeAll(allowedFields);
        fieldWhiteList.forEach(paramsNames::remove);
        if (!paramsNames.isEmpty()) {
            throw new InvalidParameterException("Недопустимые параметры фильтрации: " + paramsNames);
        }
    }


    @SneakyThrows
    public void validateOperations() {
        if (this.getClass() == Filter.class) {
            return;
        }
        Map<String, Set<FilterOperation>> index = filterCondition.visitWith(new FilterOperationIndexVisitor());
        Field[] fields = this.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            if (!field.isAnnotationPresent(AllowedOperations.class)) {
                continue;
            }

            String paramName = ((Supplier<String>) field.get(this)).get();

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

    //endregion


}
