package ru.korovin.packages.fasterjpa.queryparam;

import jakarta.persistence.criteria.*;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import ru.korovin.packages.fasterjpa.annotations.AllowedOperations;
import ru.korovin.packages.fasterjpa.annotations.FieldParamMapping;
import ru.korovin.packages.fasterjpa.annotations.ParamCountLimit;
import ru.korovin.packages.fasterjpa.exception.InvalidParameterException;
import ru.korovin.packages.fasterjpa.queryparam.filterInternal.*;
import ru.korovin.packages.fasterjpa.queryparam.utils.FieldTypeUtils;
import ru.korovin.packages.fasterjpa.service.Joins;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ru.korovin.packages.fasterjpa.annotations.FieldParamMapping.NO_MAPPING;
import static ru.korovin.packages.fasterjpa.queryparam.factories.Filters.fb;
import static ru.korovin.packages.fasterjpa.queryparam.filterInternal.FilterOperation.IS;

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
    public static final Pattern FUNCTION_PATTERN = Pattern.compile("(.*)\\.(length\\(\\)|size\\(\\)|isEmpty\\(\\)|isNotEmpty\\(\\))");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .toFormatter();

    public static final String FILTER_NOT_FOUND_MESSAGE = "В объекте %s , не найден фильтр с именем: %s";
    protected List<FilterCondition> conditions;
    protected Class<?> entityType;
    protected List<Consumer<Root<T>>> queryConfigurers = new ArrayList<>();
    private List<String> fieldWhiteList = new ArrayList<>();
    private List<String> fetchingProperties = new ArrayList<>();

    public Filter() {
        this.conditions = new ArrayList<>();
        determineEntityType();
    }

    public Filter(List<FilterCondition> conditions) {
        this.conditions = new ArrayList<>(conditions);
        determineEntityType();
    }

    public Filter(Class<T> entityType) {
        this.entityType = entityType;
        this.conditions = new ArrayList<>();
    }

    public Filter(List<FilterCondition> conditions, Class<?> entityType) {
        this.conditions = conditions;
        this.entityType = entityType;
    }

    @SneakyThrows
    public <R extends Filter<?>> R copy() {
        R copiedFilter = (R) this.getClass().getDeclaredConstructor().newInstance();
        copiedFilter.setEntityType(entityType);
        copiedFilter.setFieldWhiteList(fieldWhiteList);
        copiedFilter.setConditions(conditions);
        return copiedFilter;
    }

    public static <T> Path<T> getNestedPath(Root<T> root, String field) {
        String[] fields = field.split("\\.");
        Path<T> path = root.get(fields[0]);
        for (int i = 1; i < fields.length; i++) {
            path = path.get(fields[i]);
        }
        return path;
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
        List<FilterCondition> filterList = new ArrayList<>();
        if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
            filterList.add(new FilterCondition(fieldName, IS, isDeleted));
        } else {
            filterList.add(new FilterCondition(fieldName, IS, isDeleted ? Is.NOT_NULL : Is.NULL));
        }
        filter.setConditions(filterList);
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
        return !conditions.isEmpty();
    }

    public boolean isUnfiltered() {
        return conditions.isEmpty();
    }


    //region Criteria API Mapping
    @Override
    public Predicate toPredicate(Root<T> root,
                                 CriteriaQuery<?> query,
                                 CriteriaBuilder cb) {
        return toPredicate(root, cb);
    }

    public Predicate toPredicate(Root<T> root,
                                 CriteriaBuilder cb) {
        if (queryConfigurers.isEmpty()) {
            configureQuery(root);
        } else {
            queryConfigurers.forEach(c -> c.accept(root));
        }
        Map<String, List<Predicate>> predicates = new HashMap<>();
        conditions.forEach(c -> {
            Predicate predicate = parsePredicate(c, root, cb);
            if (predicates.containsKey(c.property())) {
                predicates.get(c.property()).add(predicate);
            } else {
                predicates.put(c.property(), new ArrayList<>(List.of(predicate)));
            }
        });
        return collectPredicates(cb, predicates);
    }

    /**
     * Предназначен для переопределения,
     * например чтобы
     */
    protected void configureQuery(Root<T> root) {
    }

    public <R extends Filter<?>> R configureQuery(Consumer<Root<T>> queryConfigurer) {
        queryConfigurers.add(queryConfigurer);
        return _this();
    }

    public <R extends Filter<?>> R _and(Filter<?> filter) {
        this.initializeOriginalNamesMap();
        this.conditions.addAll(filter.getConditions());
        this.fieldWhiteList.addAll(
                filter.getConditions()
                        .stream()
                        .map(FilterCondition::property)
                        .toList());
        if (filter.conditionsWithNoMappedFields != null) {
            filter.conditionsWithNoMappedFields.forEach(
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

    protected Predicate collectPredicates(CriteriaBuilder cb,
                                          Map<String, List<Predicate>> predicates) {
        return cb.and(predicates.values().stream()
                .flatMap(Collection::stream)
                .toList().toArray(new Predicate[0]));
    }

    private Predicate parsePredicate(FilterCondition filter,
                                     Root<T> root,
                                     CriteriaBuilder cb) {

        String field = filter.property();
        Object value = filter.value();
        FilterOperation operation = filter.operation();

        Function function = null;
        Matcher functionMatcher = FUNCTION_PATTERN.matcher(field);
        if (functionMatcher.matches()) {
            String functionStr = functionMatcher.group(2);
            function = Function.parseByOperation(functionStr);
            field = field.substring(0, field.lastIndexOf(functionStr) - 1);
        }

        Expression<?> selection = FieldExpressionCompiler.compileToCriteria(field, cb, root);
        Field reflectionField = FieldTypeUtils.getField(entityType, field);
        try {

            return switch (operation) {
                case EQUALS_IGNORE_CASE -> parseEqualIgnoreCasePredicate(cb, selection, value.toString());
                case IS -> parseIsPredicate(cb, selection, function, Is.parse(value.toString()));
                case IS_NOT -> cb.not(parseIsPredicate(cb, selection, function, Is.parse(value.toString())));
                case EQUALS -> parseEqualPredicate(cb, selection, reflectionField, value, function, field);
                case GT, LS, GTE, LSE ->
                        parseComparisonPredicate(cb, selection, operation, reflectionField, value, function, field);
                case NOT_EQUALS -> cb.not(parseEqualPredicate(cb, selection, reflectionField, value, function, field));
                case CONTAINS -> parseContainsPredicate(cb, selection, value.toString());
                case NOT_CONTAINS -> cb.not(parseContainsPredicate(cb, selection, value.toString()));
                case LIKE -> parseLikePredicate(cb, selection, value.toString());
                case NOT_LIKE -> cb.not(parseLikePredicate(cb, selection, value.toString()));
                case IN -> parseInPredicate(cb, selection, reflectionField, (Collection<?>) value, function, field);
                case NOT_IN ->
                        cb.not(parseInPredicate(cb, selection, reflectionField, (List<Object>) value, function, field));
            };
        } catch (Exception e) {
            throw new InvalidParameterException(
                    String.format("Ошибка обработки фильтра '%s' для поля '%s': %s",
                            filter, field, e.getMessage()), e);
        }
    }

    private Predicate parseEqualIgnoreCasePredicate(CriteriaBuilder cb, Expression<?> selection, String value) {
        return cb.equal(cb.lower(getTypedExpression(selection, String.class)), value.toLowerCase());
    }

    private static Class<?> getFieldType(String field, Field reflectionField, Function function) {
        if (field.startsWith("concat")) {
            return String.class;
        }
        if (function != null) {
            if (function == Function.LENGTH || function == Function.SIZE) {
                return Long.class;
            }
        }
        return reflectionField != null ? reflectionField.getType() : null;
    }


    private static Expression<String> convertToString(CriteriaBuilder cb, Expression<?> expression) {
        if (expression.getJavaType() == String.class) {
            return (Expression<String>) expression;
        }
        // Для числовых и других типов преобразуем в строку
        return cb.toString((Expression<Character>) expression);
    }

    private Predicate parseInPredicate(CriteriaBuilder cb,
                                       Expression<?> selection,
                                       Field reflectionField,
                                       Collection<?> inValues,
                                       Function function,
                                       String field) {

        //Если есть функция size или length
        if (reflectionField != null && Collection.class.isAssignableFrom(reflectionField.getType())) {
            Class<?> elementType = getCollectionElementType(reflectionField);
            if (function != null) {
                Object[] values = inValues
                        .stream()
                        .map(v -> convertValue(v, elementType))
                        .toArray();
                return getFunctionPath(cb, selection, function).in(values);
            }

            List<Predicate> predicates = new ArrayList<>();
            for (Object inValue : inValues) {
                Object val = convertValue(inValue, elementType);
                predicates.add(cb.isMember(val, (Path<Collection>) selection));
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        }
        // Для обычных полей
        Class<?> fieldType = defineValueType(selection, field, reflectionField, function);
        Object[] values = inValues.stream()
                .map(v -> convertValue(v, fieldType))
                .toArray();
        return selection.in(values);
    }

    public static Class<?> defineValueType(Expression<?> selection, String fieldName, Field reflectionField, Function function) {
        Class<?> result;
        if (reflectionField != null) {
            result = getFieldType(fieldName, reflectionField, function);
            if (result != null) {
                return result;
            }
        }
        return selection.getJavaType();
    }

    public static Class<?> getCollectionElementType(Field field) {
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                return (Class<?>) typeArgs[0];
            }
        }
        return String.class;
    }

    @SneakyThrows
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType == null || value.getClass().equals(targetType)) {
            return value;
        }

        // Конвертация между числовыми типами
        if (Number.class.isAssignableFrom(targetType) && value instanceof Number number) {

            if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                return number.intValue();
            } else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
                return number.longValue();
            } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                return number.doubleValue();
            } else if (targetType.equals(Float.class) || targetType.equals(float.class)) {
                return number.floatValue();
            } else if (targetType.equals(Short.class) || targetType.equals(short.class)) {
                return number.shortValue();
            } else if (targetType.equals(Byte.class) || targetType.equals(byte.class)) {
                return number.byteValue();
            } else if (targetType.equals(BigDecimal.class)) {
                return new BigDecimal(number.toString());
            } else if (targetType.equals(BigInteger.class)) {
                return BigInteger.valueOf(number.longValue());
            }
        }

        // Конвертация строк в числа
        if (Number.class.isAssignableFrom(targetType) && value instanceof String) {
            String stringValue = ((String) value).trim();

            if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                return Integer.parseInt(stringValue);
            } else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
                return Long.parseLong(stringValue);
            } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                return Double.parseDouble(stringValue);
            } else if (targetType.equals(Float.class) || targetType.equals(float.class)) {
                return Float.parseFloat(stringValue);
            } else if (targetType.equals(Short.class) || targetType.equals(short.class)) {
                return Short.parseShort(stringValue);
            } else if (targetType.equals(Byte.class) || targetType.equals(byte.class)) {
                return Byte.parseByte(stringValue);
            } else if (targetType.equals(BigDecimal.class)) {
                return new BigDecimal(stringValue);
            } else if (targetType.equals(BigInteger.class)) {
                return new BigInteger(stringValue);
            }
        }

        if (value instanceof Is is) {
            return switch (is) {
                case TRUE -> true;
                case FALSE -> false;
                case NULL -> null;
                default -> throw new InvalidParameterException("Некорректное значение для операции is: " + value);
            };
        }

        if (value.getClass() != String.class) {
            throw new InvalidParameterException("Невозможно преобразовать объект типа %s в тип %s"
                    .formatted(value.getClass().getSimpleName(), targetType.getSimpleName()));
        }
        String stringValue = value.toString();
        try {
            if (targetType == Integer.class || targetType == int.class) return Integer.parseInt(stringValue);
            if (targetType == Long.class || targetType == long.class) return Long.parseLong(stringValue);
            if (targetType == Double.class || targetType == double.class) return Double.parseDouble(stringValue);
            if (targetType == Float.class || targetType == float.class) return Float.parseFloat(stringValue);
            if (targetType == Boolean.class || targetType == boolean.class) return Boolean.parseBoolean(stringValue);
            if (targetType == java.sql.Date.class)
                return java.sql.Date.valueOf(LocalDate.parse(stringValue, DATE_TIME_FORMATTER));
            if (targetType == LocalDate.class) return LocalDate.parse(stringValue, DATE_TIME_FORMATTER);
            if (targetType == LocalDateTime.class) {
                try {
                    return LocalDateTime.parse(stringValue, DATE_TIME_FORMATTER);
                } catch (Exception e) {
                    return LocalDateTime.parse(stringValue);
                }
            }
            if (targetType.isEnum()) return Enum.valueOf((Class<Enum>) targetType, stringValue);

            throw new InvalidParameterException(": " + targetType.getName());
        } catch (Exception e) {
            throw new InvalidParameterException(
                    String.format("Невозможно преобразовать '%s в %s: %s",
                            stringValue, targetType.getSimpleName(), e.getMessage()), e);
        }
    }

    private Predicate parseIsPredicate(CriteriaBuilder cb, Expression<?> selection, Function function, Is value) {
        return switch (value) {
            case TRUE -> cb.isTrue(getTypedExpression(getFunctionPath(cb, selection, function), Boolean.class));
            case FALSE -> cb.isFalse(getTypedExpression(getFunctionPath(cb, selection, function), Boolean.class));
            case NULL -> cb.isNull(selection);
            case NOT_NULL -> cb.isNotNull(selection);
        };
    }

    private Predicate parseEqualPredicate(CriteriaBuilder cb,
                                          Expression<?> selection,
                                          Field reflectionField,
                                          Object value,
                                          Function function,
                                          String field) {
        if (reflectionField != null && Collection.class.isAssignableFrom(reflectionField.getType())) {

            if (function != null) {
                return switch (function) {
                    case LENGTH, SIZE -> cb.equal(getFunctionPath(cb, selection, function),
                            convertValue(value, Long.class));
                    case IS_EMPTY, IS_NOT_EMPTY -> cb.equal(getFunctionPath(
                            cb, selection, function
                    ), convertValue(value, Boolean.class));
                };
            }
            Object convertedValue = convertValue(value, getCollectionElementType(reflectionField));
            return cb.isMember(convertedValue, (Expression<Collection>) selection);
        }
        value = convertValue(value, getFieldType(field, reflectionField, function));
        return cb.equal(getFunctionPath(cb, selection, function), value);
    }

    private Expression<?> getFunctionPath(CriteriaBuilder cb, Expression<?> current, Function function) {
        if (function == null) {
            return current;
        }
        return switch (function) {
            case LENGTH -> cb.length(getTypedExpression(current, String.class));
            case SIZE -> cb.size(getTypedExpression(current, Collection.class));
            case IS_EMPTY -> cb.isEmpty(getTypedExpression(current, Collection.class));
            case IS_NOT_EMPTY -> cb.isNotEmpty(getTypedExpression(current, Collection.class));
        };

    }

    private Predicate parseComparisonPredicate(CriteriaBuilder cb,
                                               Expression<?> selection,
                                               FilterOperation operation,
                                               Field reflectionField,
                                               Object value,
                                               Function function,
                                               String field) {
        if (reflectionField != null && !Comparable.class.isAssignableFrom(reflectionField.getType())
                && function == null) {
            throw new InvalidParameterException("Аттрибут выборки " + selection + " не реализует интерфейс Comparable");
        }

        Expression<Comparable> comparablePath = (Expression<Comparable>) getFunctionPath(cb, selection, function);

        if (reflectionField != null && Collection.class.isAssignableFrom(reflectionField.getType())) {
            if (function != null) {
                return switch (function) {
                    case LENGTH, SIZE ->
                            getComparisonPredicate(cb, operation, comparablePath, (Long) convertValue(value, Long.class));
                    case IS_EMPTY, IS_NOT_EMPTY ->
                            throw new IllegalStateException("Невозможно применить операцию сравнения с функциями isEmpty()/isNotEmpty()");
                };
            }

            Comparable<?> convertedValue = (Comparable<?>) convertValue(value, getCollectionElementType(reflectionField));
            return cb.isMember(convertedValue, (Path<Collection>) selection);
        }

        Class<?> type = getFieldType(field, reflectionField, function);
        Comparable<?> comparableValue = (Comparable<?>) convertValue(value, type);
        return getComparisonPredicate(cb, operation, comparablePath, comparableValue);
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

    private Map<String, Set<FilterCondition>> fieldFiltersIndex() {
        Map<String, Set<FilterCondition>> index = new HashMap<>();
        for (var operation : conditions) {
            if (index.containsKey(operation.property())) {
                index.get(operation.property()).add(operation);
            } else {
                index.put(operation.property(), new LinkedHashSet<>(Set.of(operation)));
            }
        }
        return index;
    }

    private Map<String, Set<FilterOperation>> fieldOperationIndex() {
        Map<String, Set<FilterOperation>> index = new HashMap<>();
        for (var operation : conditions) {
            if (index.containsKey(operation.property())) {
                index.get(operation.property()).add(operation.operation());
            } else {
                index.put(operation.property(), new HashSet<>(Set.of(operation.operation())));
            }
        }
        return index;
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

    public void applyAllies() {
        if (this.getClass() == Filter.class) {
            return;
        }
        initializeOriginalNamesMap();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            FieldParamMapping fieldParamMapping = field.getAnnotation(FieldParamMapping.class);
            if (fieldParamMapping == null
                    || fieldParamMapping.sqlMapping().equals(NO_MAPPING)) {
                continue;
            }
            String alliesName = fieldParamMapping.sqlMapping();
            String fieldName = Objects.equals(fieldParamMapping.requestParamMapping(), NO_MAPPING)
                    ? field.getName() : fieldParamMapping.requestParamMapping();
            String regexSafeFieldName = Pattern.quote(fieldName);

            for (int i = 0; i < conditions.size(); i++) {
                FilterCondition op = conditions.get(i);

                if (fieldName.equals(op.property())) {
                    conditions.set(i, new FilterCondition(
                            op.property().replaceFirst(regexSafeFieldName, alliesName),
                            op.operation(),
                            op.value()));
                }

            }
        }
    }

    public void validateFields() {
        if (this.getClass() == Filter.class) {
            return;
        }
        ParamCountLimit limit;
        if ((limit = this.getClass().getAnnotation(ParamCountLimit.class)) != null
                && limit.value() != ParamCountLimit.UNLIMITED
                && conditions.size() > limit.value()) {
            throw new InvalidParameterException("Недопустимое общее кол-во фильтров: " + conditions.size()
                    + ". Допустимое значение: " + limit.value());
        }
        initializeOriginalNamesMap();
        Set<String> paramsNames = new HashSet<>(conditionsWithNoMappedFields.keySet());

        Field[] declaredFields = this.getClass().getDeclaredFields();
        Set<String> allowedFields = Arrays.stream(declaredFields)
                .map(f -> {
                    FieldParamMapping allies;
                    String paramName;
                    if ((allies = f.getAnnotation(FieldParamMapping.class)) != null
                            && !Objects.equals(allies.requestParamMapping(), NO_MAPPING)) {
                        paramName = allies.requestParamMapping();
                    } else {
                        paramName = f.getName();
                    }

                    ParamCountLimit paramLimit = f.getAnnotation(ParamCountLimit.class);
                    if (paramLimit != null && containsFilterWithField(paramName)
                            && getFiltersByFieldName(paramName, Set::of).size() > paramLimit.value()) {
                        throw new InvalidParameterException("Недопустимое кол-во фильтров для параметра %s: "
                                .formatted(paramName) + conditions.size() + ". Допустимое значение: " + paramLimit.value());
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

    private void initializeOriginalNamesMap() {
        if (conditionsWithNoMappedFields == null) {
            conditionsWithNoMappedFields = fieldFiltersIndex();
        }
    }

    public void validateOperations() {
        if (this.getClass() == Filter.class) {
            return;
        }
        Map<String, Set<FilterOperation>> index = fieldOperationIndex();
        Field[] fields = this.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (!field.isAnnotationPresent(AllowedOperations.class)) {
                continue;
            }

            String originalName = field.getName();
            String checkingName = field.getName();
            if (field.isAnnotationPresent(FieldParamMapping.class)) {
                FieldParamMapping fieldParamMapping = field.getAnnotation(FieldParamMapping.class);
                if (!fieldParamMapping.sqlMapping().equals(NO_MAPPING)) {
                    checkingName = fieldParamMapping.sqlMapping();
                }
                if (!fieldParamMapping.requestParamMapping().equals(NO_MAPPING)) {
                    originalName = fieldParamMapping.requestParamMapping();
                }
            }

            AllowedOperations allowedOperationsAnnotation = field.getAnnotation(AllowedOperations.class);

            if (index.containsKey(checkingName)) {
                Set<FilterOperation> usedOperations = index.get(checkingName);
                Set<FilterOperation> allowedOperations = Arrays.stream(allowedOperationsAnnotation.value())
                        .collect(Collectors.toSet());

                for (FilterOperation usedOp : usedOperations) {
                    if (!allowedOperations.contains(usedOp)) {
                        throw new InvalidParameterException("Недопустимая операция " + usedOp + " для параметра " + originalName);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Filter = AND" + conditions;
    }

    //endregion


}
